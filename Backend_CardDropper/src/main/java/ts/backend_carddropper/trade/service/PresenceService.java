package ts.backend_carddropper.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ce service gère la présence des utilisateurs dans les sessions de trade.
 * Il maintient une liste des utilisateurs connectés et broadcast cette liste à tous les clients via WebSocket à chaque
 * changement de présence. Il est utilisé par le RestTradeSession pour fournir la liste des utilisateurs en ligne via
 * HTTP et par le WsTradeSession pour notifier les clients en temps réel.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;

    // keycloakId -> user info
    private final Map<String, OnlineUser> onlineUsers = new ConcurrentHashMap<>();

    // Appelé par le RestTradeSession lors du join d'une session de trade
    public void userConnected(String keycloakId, String username, Long userId) {
        onlineUsers.put(keycloakId, new OnlineUser(userId, username));
        log.info("User connected to trade: {} ({})", username, keycloakId);
        broadcastPresence();
    }

    // Appelé par le RestTradeSession lors du leave d'une session de trade ou par le WsTradeSession lors de la déconnexion WebSocket
    public void userDisconnected(String keycloakId) {
        OnlineUser removed = onlineUsers.remove(keycloakId);
        if (removed != null) {
            log.info("User disconnected from trade: {} ({})", removed.username(), keycloakId);
            broadcastPresence();
        }
    }

    // Permet de récupérer la liste des utilisateurs en ligne (sans le keycloakId pour des raisons de confidentialité)
    // pour l'afficher dans le frontend
    public List<Map<String, Object>> getOnlineUsersList() {
        return onlineUsers.entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "userId", e.getValue().userId(),
                        "username", e.getValue().username()
                ))
                .toList();
    }

    // Envoie la liste des utilisateurs en ligne à tous les clients abonnés à /topic/trade/presence pour qu'ils
    // puissent mettre à jour l'affichage de la présence en temps réel
    private void broadcastPresence() {
        messagingTemplate.convertAndSend("/topic/trade/presence", getOnlineUsersList());
    }

    private record OnlineUser(Long userId, String username) {}
}
