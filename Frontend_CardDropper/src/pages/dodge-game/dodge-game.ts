import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  signal,
  inject,
  HostListener,
} from '@angular/core';
import { DodgeService } from '../../app/services/dodge.service';
import { AuthService } from '../../app/core/auth/auth.service';
import type { DodgeScore } from '../../app/models';

type ProjectileType = 'normal' | 'fast' | 'big' | 'homing' | 'splitter';

interface Projectile {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  color: string;
  trail: { x: number; y: number }[];
  type: ProjectileType;
  age: number;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  radius: number;
}

// Difficulty tiers — recalculated every 100 score points
const BASE_SPAWN_INTERVAL = 800;
const BASE_SPEED = 100;
const SPAWN_DECREASE_PER_TIER = 100;
const SPEED_INCREASE_PER_TIER = 20;
const MIN_SPAWN_INTERVAL = 220;

@Component({
  selector: 'app-dodge-game',
  templateUrl: './dodge-game.html',
  styleUrl: './dodge-game.scss',
})
export class DodgeGamePage implements AfterViewInit, OnDestroy {
  @ViewChild('gameCanvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly dodgeService = inject(DodgeService);
  private readonly auth = inject(AuthService);

  protected readonly score = signal(0);
  protected readonly highScore = signal(0);
  protected readonly gameState = signal<'menu' | 'playing' | 'dead'>('menu');

  private ctx!: CanvasRenderingContext2D;
  private animFrameId = 0;
  private lastTime = 0;
  private spawnTimer = 0;
  private scoreTimer = 0;
  private lastDifficultyTier = 0;
  private gameTime = 0;

  private player = { x: 0, y: 0, radius: 14 };
  private playerTrail: { x: number; y: number; age: number }[] = [];
  private projectiles: Projectile[] = [];
  private particles: Particle[] = [];
  private mouseX = 0;
  private mouseY = 0;
  private canvasWidth = 0;
  private canvasHeight = 0;

  private spawnInterval = BASE_SPAWN_INTERVAL;
  private projectileSpeed = BASE_SPEED;
  private isTouchDevice = false;
  private leaderboard: DodgeScore[] = [];
  private gameToken: string | null = null;

  // tier flash
  private tierFlashTimer = 0;
  private tierFlashText = '';

  // screen shake
  private shakeIntensity = 0;
  // near-miss sparks
  private readonly NEAR_MISS_DIST = 30;

  private readonly COLORS = {
    bg: '#1B1F2A',
    player: '#F0B232',
    playerGlow: 'rgba(240, 178, 50, 0.4)',
    playerTrail: '#F0B232',
    projectiles: ['#FF4444', '#FF6B6B', '#E53935', '#D32F2F', '#C62828', '#9C27B0', '#7B1FA2'],
    text: '#E0E4ED',
    textMuted: 'rgba(224, 228, 237, 0.5)',
    gold: '#F0B232',
    goldDeep: '#C8880A',
    surface: '#2A3040',
    border: 'rgba(160, 175, 210, 0.25)',
    panelBg: 'rgba(34, 40, 56, 0.95)',
  };

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    this.resizeCanvas();
    this.fetchLeaderboard();
    this.fetchMyScore();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animFrameId);
  }

  @HostListener('window:resize')
  onResize(): void {
    this.resizeCanvas();
    if (this.gameState() === 'menu') this.drawMenu();
    if (this.gameState() === 'dead') this.drawDeathScreen();
  }

  onCanvasMouseMove(e: MouseEvent): void {
    if (this.gameState() !== 'playing') return;
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    this.mouseX = e.clientX - rect.left;
    this.mouseY = e.clientY - rect.top;
  }

  onCanvasTouchMove(e: TouchEvent): void {
    if (this.gameState() !== 'playing') return;
    e.preventDefault();
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const touch = e.touches[0];
    this.mouseX = touch.clientX - rect.left;
    this.mouseY = touch.clientY - rect.top;
  }

  onCanvasTouchStart(e: TouchEvent): void {
    this.isTouchDevice = true;
    if (this.gameState() === 'menu' || this.gameState() === 'dead') {
      this.startGame();
      return;
    }
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const touch = e.touches[0];
    this.mouseX = touch.clientX - rect.left;
    this.mouseY = touch.clientY - rect.top;
  }

  onCanvasClick(): void {
    if (this.gameState() === 'menu' || this.gameState() === 'dead') {
      this.startGame();
    }
  }

  startGame(): void {
    this.score.set(0);
    this.projectiles = [];
    this.particles = [];
    this.playerTrail = [];
    this.spawnInterval = BASE_SPAWN_INTERVAL;
    this.projectileSpeed = BASE_SPEED;
    this.spawnTimer = 0;
    this.scoreTimer = 0;
    this.lastDifficultyTier = 0;
    this.gameTime = 0;
    this.tierFlashTimer = 0;
    this.shakeIntensity = 0;
    this.gameToken = null;
    this.player.x = this.canvasWidth / 2;
    this.player.y = this.canvasHeight / 2;
    this.mouseX = this.player.x;
    this.mouseY = this.player.y;
    this.gameState.set('playing');
    this.lastTime = performance.now();
    this.animFrameId = requestAnimationFrame((t) => this.gameLoop(t));

    // request signed token from backend
    if (this.auth.isAuthenticated) {
      this.dodgeService.startGame().subscribe({
        next: (res) => this.gameToken = res.token,
      });
    }
  }

  // ── Backend integration ──

  private fetchLeaderboard(): void {
    if (!this.auth.isAuthenticated) {
      this.drawMenu();
      return;
    }
    this.dodgeService.getLeaderboard().subscribe({
      next: (scores) => {
        this.leaderboard = scores;
        if (this.gameState() === 'menu') this.drawMenu();
      },
      error: () => this.drawMenu(),
    });
  }

  private fetchMyScore(): void {
    if (!this.auth.isAuthenticated) return;
    this.dodgeService.getMyScore().subscribe({
      next: (score) => {
        this.highScore.set(score.bestScore);
        if (this.gameState() === 'menu') this.drawMenu();
      },
    });
  }

  private submitScore(score: number): void {
    if (!this.auth.isAuthenticated || !this.gameToken) return;
    this.dodgeService.submitScore(this.gameToken, score).subscribe({
      next: (result) => {
        this.highScore.set(result.bestScore);
        this.dodgeService.getLeaderboard().subscribe({
          next: (scores) => {
            this.leaderboard = scores;
            if (this.gameState() === 'dead') this.drawDeathScreen();
          },
        });
      },
    });
  }

  // ── Game loop ──

  private resizeCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    const container = canvas.parentElement!;
    this.canvasWidth = container.clientWidth;
    this.canvasHeight = container.clientHeight;
    canvas.width = this.canvasWidth;
    canvas.height = this.canvasHeight;
    this.player.x = this.canvasWidth / 2;
    this.player.y = this.canvasHeight / 2;
  }

  private gameLoop(time: number): void {
    if (this.gameState() !== 'playing') return;
    const dt = Math.min(time - this.lastTime, 50);
    this.lastTime = time;
    this.update(dt);
    if (this.gameState() !== 'playing') return;
    this.draw();
    this.animFrameId = requestAnimationFrame((t) => this.gameLoop(t));
  }

  private update(dt: number): void {
    this.gameTime += dt;

    // smooth player follow cursor
    const lerp = 1 - Math.pow(0.00001, dt / 1000);
    this.player.x += (this.mouseX - this.player.x) * lerp;
    this.player.y += (this.mouseY - this.player.y) * lerp;

    // clamp to canvas
    this.player.x = Math.max(this.player.radius, Math.min(this.canvasWidth - this.player.radius, this.player.x));
    this.player.y = Math.max(this.player.radius, Math.min(this.canvasHeight - this.player.radius, this.player.y));

    // player trail
    this.playerTrail.push({ x: this.player.x, y: this.player.y, age: 0 });
    for (const t of this.playerTrail) t.age += dt;
    this.playerTrail = this.playerTrail.filter(t => t.age < 200);

    // score: +1 per 100ms survived
    this.scoreTimer += dt;
    while (this.scoreTimer >= 100) {
      this.scoreTimer -= 100;
      this.score.update((s) => s + 1);
    }

    // difficulty ramp
    const currentTier = Math.floor(this.score() / 100);
    if (currentTier > this.lastDifficultyTier) {
      this.lastDifficultyTier = currentTier;
      this.spawnInterval = Math.max(MIN_SPAWN_INTERVAL, BASE_SPAWN_INTERVAL - currentTier * SPAWN_DECREASE_PER_TIER);
      this.projectileSpeed = BASE_SPEED + currentTier * SPEED_INCREASE_PER_TIER;
      this.tierFlashTimer = 1500;
      this.tierFlashText = `Niveau ${currentTier} !`;
    }

    // tier flash decay
    if (this.tierFlashTimer > 0) this.tierFlashTimer -= dt;

    // screen shake decay
    if (this.shakeIntensity > 0) {
      this.shakeIntensity *= Math.pow(0.002, dt / 1000);
      if (this.shakeIntensity < 0.5) this.shakeIntensity = 0;
    }

    // spawn projectiles
    this.spawnTimer += dt;
    while (this.spawnTimer >= this.spawnInterval) {
      this.spawnTimer -= this.spawnInterval;
      this.spawnProjectile();
    }

    // update projectiles
    const newProjectiles: Projectile[] = [];
    for (const p of this.projectiles) {
      p.age += dt;

      // homing: gently steer toward player for first 2s
      if (p.type === 'homing' && p.age < 2000) {
        const toPlayerX = this.player.x - p.x;
        const toPlayerY = this.player.y - p.y;
        const dist = Math.sqrt(toPlayerX * toPlayerX + toPlayerY * toPlayerY);
        if (dist > 0) {
          const steer = 1.8 * (dt / 1000);
          p.vx += (toPlayerX / dist) * this.projectileSpeed * steer;
          p.vy += (toPlayerY / dist) * this.projectileSpeed * steer;
          // cap speed
          const currentSpeed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
          const maxSpeed = this.projectileSpeed * 0.9;
          if (currentSpeed > maxSpeed) {
            p.vx = (p.vx / currentSpeed) * maxSpeed;
            p.vy = (p.vy / currentSpeed) * maxSpeed;
          }
        }
      }

      // splitter: split after 1s of travel
      if (p.type === 'splitter' && p.age > 2000) {
        this.spawnSplitterChildren(p);
        continue; // remove the parent
      }

      p.x += p.vx * (dt / 1000);
      p.y += p.vy * (dt / 1000);
      p.trail.push({ x: p.x, y: p.y });
      if (p.trail.length > 10) p.trail.shift();
      newProjectiles.push(p);
    }
    this.projectiles = newProjectiles;

    // remove offscreen
    const margin = 60;
    this.projectiles = this.projectiles.filter(
      (p) =>
        p.x > -margin &&
        p.x < this.canvasWidth + margin &&
        p.y > -margin &&
        p.y < this.canvasHeight + margin
    );

    // collision + near-miss check
    for (const p of this.projectiles) {
      const dx = p.x - this.player.x;
      const dy = p.y - this.player.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dist < p.radius + this.player.radius - 2) {
        this.die();
        return;
      }

      // near-miss sparks
      if (dist < this.NEAR_MISS_DIST + p.radius + this.player.radius) {
        if (Math.random() < 0.15) {
          const midX = (p.x + this.player.x) / 2;
          const midY = (p.y + this.player.y) / 2;
          this.particles.push({
            x: midX, y: midY,
            vx: (Math.random() - 0.5) * 120,
            vy: (Math.random() - 0.5) * 120,
            life: 200 + Math.random() * 200,
            maxLife: 400,
            color: '#FFD369',
            radius: 1.5 + Math.random() * 2,
          });
        }
      }
    }

    // update particles
    for (const p of this.particles) {
      p.x += p.vx * (dt / 1000);
      p.y += p.vy * (dt / 1000);
      p.life -= dt;
    }
    this.particles = this.particles.filter((p) => p.life > 0);
  }

  private pickProjectileType(): ProjectileType {
    const tier = this.lastDifficultyTier;
    const roll = Math.random();

    // tier 0: normal only
    // tier 1+: fast (20%)
    // tier 2+: big (15%)
    // tier 3+: homing (12%)
    // tier 5+: splitter (8%)
    if (tier >= 8 && roll < 0.08) return 'splitter';
    if (tier >= 6 && roll < 0.20) return 'homing';
    if (tier >= 4 && roll < 0.35) return 'big';
    if (tier >= 2 && roll < 0.55) return 'fast';
    return 'normal';
  }

  private spawnProjectile(): void {
    const edge = Math.floor(Math.random() * 4);
    let x: number, y: number;
    switch (edge) {
      case 0: x = Math.random() * this.canvasWidth; y = -20; break;
      case 1: x = this.canvasWidth + 20; y = Math.random() * this.canvasHeight; break;
      case 2: x = Math.random() * this.canvasWidth; y = this.canvasHeight + 20; break;
      default: x = -20; y = Math.random() * this.canvasHeight; break;
    }

    const type = this.pickProjectileType();
    const angle = Math.atan2(this.player.y - y, this.player.x - x) + (Math.random() - 0.5) * 0.6;

    let speed: number, radius: number, color: string;
    switch (type) {
      case 'fast':
        speed = this.projectileSpeed * 1.6;
        radius = 4 + Math.random() * 3;
        color = '#00E5FF';
        break;
      case 'big':
        speed = this.projectileSpeed * 0.6;
        radius = 14 + Math.random() * 6;
        color = '#FF6F00';
        break;
      case 'homing':
        speed = this.projectileSpeed * 0.75;
        radius = 7 + Math.random() * 3;
        color = '#76FF03';
        break;
      case 'splitter':
        speed = this.projectileSpeed * 0.8;
        radius = 10 + Math.random() * 4;
        color = '#E040FB';
        break;
      default:
        speed = this.projectileSpeed * (0.8 + Math.random() * 0.4);
        radius = 6 + Math.random() * 5;
        color = this.COLORS.projectiles[Math.floor(Math.random() * this.COLORS.projectiles.length)];
    }

    this.projectiles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      radius, color, type,
      trail: [],
      age: 0,
    });
  }

  private spawnSplitterChildren(parent: Projectile): void {
    for (let i = 0; i < 3; i++) {
      const angle = Math.atan2(parent.vy, parent.vx) + (i - 1) * 0.8;
      const speed = this.projectileSpeed * 0.9;
      this.projectiles.push({
        x: parent.x, y: parent.y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        radius: 5,
        color: '#CE93D8',
        type: 'normal',
        trail: [],
        age: 0,
      });
    }
  }

  private die(): void {
    cancelAnimationFrame(this.animFrameId);
    this.gameState.set('dead');

    const currentScore = this.score();
    if (currentScore > this.highScore()) {
      this.highScore.set(currentScore);
    }

    this.submitScore(currentScore);

    // death explosion — lots of particles
    for (let i = 0; i < 50; i++) {
      const angle = Math.random() * Math.PI * 2;
      const speed = 60 + Math.random() * 280;
      const isGold = Math.random() > 0.3;
      this.particles.push({
        x: this.player.x,
        y: this.player.y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        life: 600 + Math.random() * 800,
        maxLife: 1400,
        color: isGold ? this.COLORS.gold : '#FF6B6B',
        radius: 2 + Math.random() * 5,
      });
    }

    // screen shake
    this.shakeIntensity = 12;

    // animate death screen with particles settling
    this.animateDeathScreen();
  }

  private animateDeathScreen(): void {
    if (this.gameState() !== 'dead') return;

    const dt = 16;
    let hasLivingParticles = false;

    for (const p of this.particles) {
      p.x += p.vx * (dt / 1000);
      p.y += p.vy * (dt / 1000);
      p.vy += 80 * (dt / 1000); // gravity
      p.life -= dt;
      if (p.life > 0) hasLivingParticles = true;
    }
    this.particles = this.particles.filter((p) => p.life > 0);

    if (this.shakeIntensity > 0) {
      this.shakeIntensity *= 0.9;
      if (this.shakeIntensity < 0.5) this.shakeIntensity = 0;
    }

    this.drawDeathScreen();

    if (hasLivingParticles || this.shakeIntensity > 0) {
      this.animFrameId = requestAnimationFrame(() => this.animateDeathScreen());
    }
  }

  // ── Panel drawing helpers ──

  private drawPanel(cx: number, cy: number, pw: number, ph: number): void {
    const ctx = this.ctx;
    const x = cx - pw / 2;
    const y = cy - ph / 2;
    const r = 12;

    ctx.save();
    ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
    ctx.shadowBlur = 32;
    ctx.shadowOffsetY = 8;

    ctx.beginPath();
    ctx.roundRect(x, y, pw, ph, r);
    ctx.fillStyle = this.COLORS.panelBg;
    ctx.fill();
    ctx.restore();

    ctx.beginPath();
    ctx.roundRect(x, y, pw, ph, r);
    ctx.strokeStyle = this.COLORS.border;
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.beginPath();
    ctx.roundRect(x + 1, y + 1, pw - 2, 3, [r, r, 0, 0]);
    ctx.fillStyle = this.COLORS.gold;
    ctx.fill();
  }

  private drawLeaderboard(cx: number, startY: number): void {
    const ctx = this.ctx;
    ctx.textAlign = 'center';

    if (this.leaderboard.length === 0) {
      ctx.fillStyle = this.COLORS.textMuted;
      ctx.font = '14px Inter, system-ui, sans-serif';
      ctx.fillText('Aucun score cette semaine', cx, startY);
      return;
    }

    for (let i = 0; i < this.leaderboard.length; i++) {
      const y = startY + i * 24;
      const entry = this.leaderboard[i];
      const isTop = i === 0;

      ctx.fillStyle = isTop ? this.COLORS.gold : this.COLORS.textMuted;
      ctx.font = isTop ? 'bold 15px Inter, system-ui, sans-serif' : '14px Inter, system-ui, sans-serif';
      ctx.fillText(`#${i + 1}  ${entry.username}  —  ${entry.bestScore} pts`, cx, y);
    }
  }

  // ── Drawing ──

  private draw(): void {
    const ctx = this.ctx;
    const w = this.canvasWidth;
    const h = this.canvasHeight;

    // apply screen shake
    ctx.save();
    if (this.shakeIntensity > 0) {
      const sx = (Math.random() - 0.5) * this.shakeIntensity * 2;
      const sy = (Math.random() - 0.5) * this.shakeIntensity * 2;
      ctx.translate(sx, sy);
    }

    this.drawBackground();

    // danger vignette — intensifies with tier
    const dangerAlpha = Math.min(this.lastDifficultyTier * 0.02, 0.15);
    if (dangerAlpha > 0) {
      const vignette = ctx.createRadialGradient(w / 2, h / 2, w * 0.3, w / 2, h / 2, w * 0.8);
      vignette.addColorStop(0, 'rgba(0, 0, 0, 0)');
      vignette.addColorStop(1, `rgba(180, 30, 30, ${dangerAlpha})`);
      ctx.fillStyle = vignette;
      ctx.fillRect(0, 0, w, h);
    }

    // player trail
    for (let i = 0; i < this.playerTrail.length; i++) {
      const t = this.playerTrail[i];
      const progress = 1 - t.age / 200;
      const alpha = progress * 0.3;
      const r = this.player.radius * progress * 0.7;
      if (r < 1) continue;
      ctx.beginPath();
      ctx.arc(t.x, t.y, r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(240, 178, 50, ${alpha})`;
      ctx.fill();
    }

    // projectile trails + projectiles
    for (const p of this.projectiles) {
      // trail
      if (p.trail.length > 1) {
        for (let i = 1; i < p.trail.length; i++) {
          const alpha = (i / p.trail.length) * 0.5;
          const r = p.radius * (i / p.trail.length) * 0.5;
          ctx.beginPath();
          ctx.arc(p.trail[i].x, p.trail[i].y, r, 0, Math.PI * 2);
          ctx.fillStyle = p.color + Math.floor(alpha * 255).toString(16).padStart(2, '0');
          ctx.fill();
        }
      }

      // outer glow
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius + 6, 0, Math.PI * 2);
      const outerGlow = ctx.createRadialGradient(p.x, p.y, p.radius * 0.5, p.x, p.y, p.radius + 6);
      outerGlow.addColorStop(0, p.color + '44');
      outerGlow.addColorStop(1, p.color + '00');
      ctx.fillStyle = outerGlow;
      ctx.fill();

      // core
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fillStyle = p.color;
      ctx.fill();

      // type-specific inner visual
      if (p.type === 'fast') {
        // speed lines — elongated bright streak
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius * 0.5, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255, 255, 255, 0.5)';
        ctx.fill();
      } else if (p.type === 'homing') {
        // pulsing ring
        const pulse = Math.sin(p.age * 0.008) * 0.3 + 0.7;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius + 2, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(118, 255, 3, ${pulse * 0.6})`;
        ctx.lineWidth = 2;
        ctx.stroke();
      } else if (p.type === 'big') {
        // inner cracks pattern
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius * 0.6, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255, 200, 50, 0.3)';
        ctx.fill();
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius * 0.25, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
        ctx.fill();
      } else if (p.type === 'splitter') {
        // countdown shimmer — pulses faster as it's about to split
        const timeLeft = Math.max(0, 1000 - p.age);
        const pulseSpeed = 0.005 + (1 - timeLeft / 1000) * 0.02;
        const shimmer = Math.sin(p.age * pulseSpeed) * 0.4 + 0.6;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius * shimmer, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(224, 64, 251, 0.7)`;
        ctx.lineWidth = 2;
        ctx.stroke();
        // inner dots showing it'll split
        for (let d = 0; d < 3; d++) {
          const a = (d / 3) * Math.PI * 2 + p.age * 0.003;
          const dx = Math.cos(a) * p.radius * 0.5;
          const dy = Math.sin(a) * p.radius * 0.5;
          ctx.beginPath();
          ctx.arc(p.x + dx, p.y + dy, 2, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(255, 255, 255, 0.5)';
          ctx.fill();
        }
      } else {
        // normal — simple bright center
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius * 0.4, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.fill();
      }
    }

    // particles
    for (const p of this.particles) {
      const alpha = p.life / p.maxLife;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius * alpha, 0, Math.PI * 2);
      ctx.fillStyle = p.color + Math.floor(alpha * 255).toString(16).padStart(2, '0');
      ctx.fill();
    }

    // player breathing glow
    const breathe = Math.sin(this.gameTime * 0.003) * 0.15 + 0.85;
    const glowRadius = (this.player.radius + 12) * breathe;
    ctx.beginPath();
    ctx.arc(this.player.x, this.player.y, glowRadius, 0, Math.PI * 2);
    const playerGlow = ctx.createRadialGradient(
      this.player.x, this.player.y, this.player.radius * 0.5,
      this.player.x, this.player.y, glowRadius
    );
    playerGlow.addColorStop(0, `rgba(240, 178, 50, ${0.35 * breathe})`);
    playerGlow.addColorStop(1, 'rgba(240, 178, 50, 0)');
    ctx.fillStyle = playerGlow;
    ctx.fill();

    // player body
    ctx.beginPath();
    ctx.arc(this.player.x, this.player.y, this.player.radius, 0, Math.PI * 2);
    const bodyGrad = ctx.createRadialGradient(
      this.player.x - 3, this.player.y - 3, 2,
      this.player.x, this.player.y, this.player.radius
    );
    bodyGrad.addColorStop(0, '#FFD369');
    bodyGrad.addColorStop(1, this.COLORS.player);
    ctx.fillStyle = bodyGrad;
    ctx.fill();

    // player ring
    ctx.beginPath();
    ctx.arc(this.player.x, this.player.y, this.player.radius, 0, Math.PI * 2);
    ctx.strokeStyle = `rgba(255, 211, 105, ${0.6 + 0.4 * breathe})`;
    ctx.lineWidth = 2;
    ctx.stroke();

    // HUD
    ctx.fillStyle = this.COLORS.text;
    ctx.font = 'bold 22px Inter, system-ui, sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`Score: ${this.score()}`, 16, 32);

    ctx.fillStyle = this.COLORS.textMuted;
    ctx.font = '14px Inter, system-ui, sans-serif';
    ctx.fillText(`Meilleur: ${this.highScore()}`, 16, 52);

    const tier = this.lastDifficultyTier;
    if (tier > 0) {
      ctx.fillStyle = this.COLORS.gold;
      ctx.font = 'bold 13px Inter, system-ui, sans-serif';
      ctx.fillText(`Niveau ${tier}`, 16, 72);
    }

    // tier flash — small animation under HUD
    if (this.tierFlashTimer > 0) {
      const flashAlpha = Math.min(this.tierFlashTimer / 500, 1) * 0.9;
      ctx.save();
      ctx.textAlign = 'left';
      ctx.fillStyle = `rgba(240, 178, 50, ${flashAlpha})`;
      ctx.font = 'bold 18px Inter, system-ui, sans-serif';
      const yOffset = (1 - this.tierFlashTimer / 1500) * -8;
      ctx.fillText(this.tierFlashText, 16, 92 + yOffset);
      ctx.restore();
    }

    ctx.restore(); // end screen shake transform
  }

  private drawBackground(): void {
    const ctx = this.ctx;
    const w = this.canvasWidth;
    const h = this.canvasHeight;

    ctx.fillStyle = this.COLORS.bg;
    ctx.fillRect(-10, -10, w + 20, h + 20); // extra to cover shake offset

    ctx.strokeStyle = 'rgba(160, 175, 210, 0.05)';
    ctx.lineWidth = 1;
    const gridSize = 40;
    for (let x = 0; x < w; x += gridSize) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
    }
    for (let y = 0; y < h; y += gridSize) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }
  }

  private drawMenu(): void {
    const ctx = this.ctx;
    const w = this.canvasWidth;
    const h = this.canvasHeight;

    this.drawBackground();

    const hasLeaderboard = this.leaderboard.length > 0;
    const leaderboardRows = Math.max(this.leaderboard.length, 1);
    const panelW = Math.min(380, w - 40);
    const panelH = hasLeaderboard
      ? 280 + leaderboardRows * 24
      : this.auth.isAuthenticated ? 280 : 200;
    const panelCy = h / 2;

    this.drawPanel(w / 2, panelCy, panelW, panelH);

    const top = panelCy - panelH / 2;
    ctx.textAlign = 'center';

    ctx.fillStyle = this.COLORS.gold;
    ctx.font = 'bold 36px Inter, system-ui, sans-serif';
    ctx.fillText('CardDodger', w / 2, top + 50);

    ctx.fillStyle = this.COLORS.text;
    ctx.font = '15px Inter, system-ui, sans-serif';
    ctx.fillText('Esquive les projectiles le plus', w / 2, top + 80);
    ctx.fillText('longtemps possible !', w / 2, top + 98);

    if (this.auth.isAuthenticated && this.highScore() > 0) {
      ctx.fillStyle = this.COLORS.gold;
      ctx.font = 'bold 14px Inter, system-ui, sans-serif';
      ctx.fillText(`Ton meilleur score : ${this.highScore()} pts`, w / 2, top + 124);
    }

    const divY = this.auth.isAuthenticated ? top + 140 : top + 118;
    ctx.beginPath();
    ctx.moveTo(w / 2 - panelW / 2 + 24, divY);
    ctx.lineTo(w / 2 + panelW / 2 - 24, divY);
    ctx.strokeStyle = this.COLORS.border;
    ctx.lineWidth = 1;
    ctx.stroke();

    if (this.auth.isAuthenticated) {
      ctx.fillStyle = this.COLORS.textMuted;
      ctx.font = 'bold 13px Inter, system-ui, sans-serif';
      ctx.fillText('CLASSEMENT DE LA SEMAINE', w / 2, divY + 24);
      this.drawLeaderboard(w / 2, divY + 50);
    }

    ctx.fillStyle = this.COLORS.textMuted;
    ctx.font = '15px Inter, system-ui, sans-serif';
    const instruction = this.isTouchDevice ? 'Touche pour commencer' : 'Clique pour commencer';
    ctx.fillText(instruction, w / 2, top + panelH - 24);
  }

  private drawDeathScreen(): void {
    const ctx = this.ctx;
    const w = this.canvasWidth;
    const h = this.canvasHeight;

    // apply shake
    ctx.save();
    if (this.shakeIntensity > 0) {
      const sx = (Math.random() - 0.5) * this.shakeIntensity * 2;
      const sy = (Math.random() - 0.5) * this.shakeIntensity * 2;
      ctx.translate(sx, sy);
    }

    // dim overlay
    ctx.fillStyle = 'rgba(27, 31, 42, 0.85)';
    ctx.fillRect(-10, -10, w + 20, h + 20);

    // draw particles
    for (const p of this.particles) {
      const alpha = p.life / p.maxLife;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius * alpha, 0, Math.PI * 2);
      ctx.fillStyle = p.color + Math.floor(alpha * 255).toString(16).padStart(2, '0');
      ctx.fill();
    }

    // panel
    const isNewRecord = this.score() >= this.highScore() && this.score() > 0;
    const leaderboardRows = Math.max(this.leaderboard.length, 1);
    const panelW = Math.min(380, w - 40);
    const panelH = this.auth.isAuthenticated
      ? 280 + leaderboardRows * 24 + (isNewRecord ? 28 : 0)
      : 200;
    const panelCy = h / 2;

    this.drawPanel(w / 2, panelCy, panelW, panelH);

    const top = panelCy - panelH / 2;
    ctx.textAlign = 'center';

    ctx.fillStyle = '#FF4444';
    ctx.font = 'bold 34px Inter, system-ui, sans-serif';
    ctx.fillText('Perdu !', w / 2, top + 48);

    ctx.fillStyle = this.COLORS.textMuted;
    ctx.font = '13px Inter, system-ui, sans-serif';
    ctx.fillText('TON SCORE', w / 2, top + 76);

    ctx.fillStyle = this.COLORS.text;
    ctx.font = 'bold 32px Inter, system-ui, sans-serif';
    ctx.fillText(`${this.score()}`, w / 2, top + 110);

    let cursorY = top + 120;

    if (isNewRecord) {
      cursorY += 6;
      ctx.fillStyle = this.COLORS.gold;
      ctx.font = 'bold 15px Inter, system-ui, sans-serif';
      ctx.fillText('Nouveau record !', w / 2, cursorY);
      cursorY += 22;
    }

    if (this.auth.isAuthenticated) {
      cursorY += 10;
      ctx.beginPath();
      ctx.moveTo(w / 2 - panelW / 2 + 24, cursorY);
      ctx.lineTo(w / 2 + panelW / 2 - 24, cursorY);
      ctx.strokeStyle = this.COLORS.border;
      ctx.lineWidth = 1;
      ctx.stroke();

      cursorY += 24;
      ctx.fillStyle = this.COLORS.textMuted;
      ctx.font = 'bold 13px Inter, system-ui, sans-serif';
      ctx.fillText('CLASSEMENT DE LA SEMAINE', w / 2, cursorY);

      cursorY += 24;
      this.drawLeaderboard(w / 2, cursorY);
    }

    ctx.fillStyle = this.COLORS.textMuted;
    ctx.font = '15px Inter, system-ui, sans-serif';
    const instruction = this.isTouchDevice ? 'Touche pour rejouer' : 'Clique pour rejouer';
    ctx.fillText(instruction, w / 2, top + panelH - 24);

    ctx.restore(); // end shake transform
  }
}
