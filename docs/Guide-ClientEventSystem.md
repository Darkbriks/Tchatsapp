# Système d'Événements - Documentation Développeur

## Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Concepts fondamentaux](#concepts-fondamentaux)
3. [Guide de démarrage rapide](#guide-de-démarrage-rapide)
4. [Créer un nouvel événement](#créer-un-nouvel-événement)
5. [S'abonner aux événements](#sabonner-aux-événements)
6. [Publier des événements](#publier-des-événements)
7. [Filtrage avancé](#filtrage-avancé)
8. [Gestion du cycle de vie](#gestion-du-cycle-de-vie)
9. [Bonnes pratiques](#bonnes-pratiques)
10. [Pièges à éviter](#pièges-à-éviter)
11. [Référence API](#référence-api)
12. [Exemples complets](#exemples-complets)

---

## Vue d'ensemble

Le système d'événements fournit un mécanisme générique, type-safe et extensible pour la communication entre composants de l'application, en particulier entre la couche logique (ClientController) et l'interface utilisateur.

```
┌─────────────────┐        ┌──────────┐        ┌─────────────┐
│ PacketHandler   │───1───>│ EventBus │<───2───│ UI Observer │
│ (Publisher)     │        │          │        │ (Subscriber)│
└─────────────────┘        └──────────┘        └─────────────┘
                                │
                                │ 3. Dispatch
                                ▼
                           ┌─────────┐
                           │ Observer│
                           │.onEvent │
                           └─────────┘

1. Publication d'un événement
2. Subscription à un type d'événement
3. Notification automatique
```

---

## Concepts fondamentaux

### Event

Classe abstraite de base pour tous les événements. Chaque événement possède :
- `timestamp` : Moment de création (en nanosecondes)
- `source` : Objet ayant émis l'événement
- `getEventType()` : Type de l'événement (pour le dispatching)

```java
public abstract class Event {
    private final long timestamp;
    private final Object source;
    
    protected Event(Object source) {
        this.timestamp = System.nanoTime();
        this.source = source;
    }
    
    public abstract Class<? extends Event> getEventType();
}
```

### EventObserver<T>

Interface fonctionnelle pour recevoir les événements d'un type spécifique.

```java
@FunctionalInterface
public interface EventObserver<T extends Event> {
    void onEvent(T event);
}
```

### EventBus

Hub central (Singleton) qui gère les subscriptions et le dispatching des événements.

**Méthodes principales** :
- `getInstance()` : Obtenir l'instance unique
- `subscribe(...)` : S'abonner à un type d'événement
- `unsubscribe(...)` : Se désabonner
- `publish(...)` : Publier un événement

### EventSubscription<T>

Représente une subscription active. Permet de gérer le cycle de vie de l'abonnement.

**Méthodes** :
- `isActive()` : Vérifie si la subscription est active
- `cancel()` : Désactive la subscription

### ExecutionMode

Enum définissant le mode d'exécution des observers.

```java
public enum ExecutionMode {
    SYNC,   // Exécution immédiate dans le thread émetteur
    ASYNC   // Exécution dans un thread pool dédié
}
```

**Règle de choix** :
- `SYNC` : Traitement rapide, accès direct à l'UI
- `ASYNC` : I/O, calculs lourds, traitement pouvant bloquer

### EventFilter<T>

Interface pour filtrer les événements avant notification.

```java
@FunctionalInterface
public interface EventFilter<T extends Event> {
    boolean test(T event);
}
```

---

## Guide de démarrage rapide

### Étape 1 : Obtenir l'instance ClientController

```java
ClientController controller = new ClientController();
controller.connect("localhost", 8080, userId);
```

### Étape 2 : S'abonner à un événement

```java
// Méthode la plus simple (mode SYNC par défaut, pas de filtre)
EventSubscription<TextMessageReceivedEvent> subscription = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> {
            Message msg = event.getMessage();
            System.out.println("Message reçu : " + msg.getContent());
        }
    );
```

### Étape 3 : Nettoyer à la fin

```java
// Lors de la fermeture du composant
controller.unsubscribe(subscription);
```

---

## Créer un nouvel événement

### Structure d'un événement

Chaque événement doit :
1. Hériter de `Event` ou d'une sous-classe existante
2. Avoir des champs `final` pour l'immutabilité
3. Implémenter `getEventType()`
4. Fournir des getters pour accéder aux données

### Exemple : Événement simple

```java
package fr.uga.im2ag.tchatapp.event.types;

import fr.uga.im2ag.tchatapp.event.system.Event;

public class UserTypingEvent extends Event {
    private final int userId;
    private final int conversationId;
    private final boolean isTyping;
    
    public UserTypingEvent(Object source, int userId, int conversationId, boolean isTyping) {
        super(source);
        this.userId = userId;
        this.conversationId = conversationId;
        this.isTyping = isTyping;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public int getConversationId() {
        return conversationId;
    }
    
    public boolean isTyping() {
        return isTyping;
    }
    
    @Override
    public Class<? extends Event> getEventType() {
        return UserTypingEvent.class;
    }
}
```

### Exemple : Événement avec hiérarchie

```java
// Classe abstraite parente
public abstract class NotificationEvent extends Event {
    private final String title;
    private final String message;
    private final NotificationPriority priority;
    
    protected NotificationEvent(Object source, String title, String message, 
                                NotificationPriority priority) {
        super(source);
        this.title = title;
        this.message = message;
        this.priority = priority;
    }
    
    // Getters...
}

// Événement spécifique
public class IncomingCallNotificationEvent extends NotificationEvent {
    private final int callerId;
    
    public IncomingCallNotificationEvent(Object source, String title, String message,
                                         NotificationPriority priority, int callerId) {
        super(source, title, message, priority);
        this.callerId = callerId;
    }
    
    public int getCallerId() {
        return callerId;
    }
    
    @Override
    public Class<? extends Event> getEventType() {
        return IncomingCallNotificationEvent.class;
    }
}
```

---

## S'abonner aux événements

### Subscription basique

```java
// Forme la plus simple
EventSubscription<TextMessageReceivedEvent> sub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> handleMessage(event.getMessage())
    );
```

### Subscription avec mode d'exécution

```java
// Mode SYNC (par défaut)
EventSubscription<TextMessageReceivedEvent> syncSub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> updateMessageCount(event),
        ExecutionMode.SYNC
    );

// Mode ASYNC pour traitement long
EventSubscription<MediaMessageReceivedEvent> asyncSub = 
    controller.subscribeToEvent(
        MediaMessageReceivedEvent.class,
        event -> {
            // Traitement long (I/O, décodage...)
            byte[] mediaData = loadMediaFromDisk(event.getMessage());
            processMedia(mediaData);
        },
        ExecutionMode.ASYNC
    );
```

### Subscription avec filtre

```java
// Filtrer par conversation
final int targetConversationId = 42;
EventSubscription<TextMessageReceivedEvent> filteredSub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> updateChatView(event),
        event -> event.getConversationId() == targetConversationId,
        ExecutionMode.SYNC
    );

// Filtrer par contenu
EventSubscription<TextMessageReceivedEvent> urgentSub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> showUrgentNotification(event),
        event -> event.getMessage().getContent().startsWith("[URGENT]"),
        ExecutionMode.ASYNC
    );
```

### Subscription avec référence de méthode

```java
public class ChatViewController {
    
    public void initialize() {
        // Utiliser une méthode existante comme observer
        EventSubscription<TextMessageReceivedEvent> sub = 
            controller.subscribeToEvent(
                TextMessageReceivedEvent.class,
                this::onMessageReceived
            );
    }
    
    private void onMessageReceived(TextMessageReceivedEvent event) {
        Message msg = event.getMessage();
        int convId = event.getConversationId();
        // Traitement...
    }
}
```

### Subscription à une hiérarchie d'événements

```java
// S'abonner à tous les événements de message
EventSubscription<MessageEvent> allMessagesSub = 
    controller.subscribeToEvent(
        MessageEvent.class,
        event -> {
            // Reçoit TextMessageReceivedEvent ET MediaMessageReceivedEvent
            logMessageActivity(event.getMessage());
        }
    );

// S'abonner uniquement aux messages texte
EventSubscription<TextMessageReceivedEvent> textMessagesSub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        event -> {
            // Reçoit uniquement les messages texte
            displayTextMessage(event.getMessage());
        }
    );
```

---

## Publier des événements

```java
public class ClientTextMessageHandler extends ClientPacketHandler {
    
    @Override
    public void handle(ProtocolMessage msg) {
        TextMessage textMsg = (TextMessage) msg;
        
        // 1. Traiter le message (logique métier)
        Message message = createMessageFromProtocol(textMsg);
        Conversation conv = client.getConversation(textMsg.getTo());
        conv.addMessage(message);
        
        // 2. Publier l'événement
        publishEvent(new TextMessageReceivedEvent(
            this,                 // source
            message,              // message reçu
            textMsg.getTo()       // conversationId
        ));
    }
    
    @Override
    public boolean canHandle(MessageType type) {
        return type == MessageType.TEXT;
    }
}
```

**Note** : Il est également possible de publier des événements dans des blocs `catch` pour signaler des erreurs.

```java
public class ClientManagementHandler extends ClientPacketHandler {
    
    @Override
    public void handle(ProtocolMessage msg) {
        ManagementMessage mgmtMsg = (ManagementMessage) msg;
        
        try {
            // Traitement...
            processManagementMessage(mgmtMsg);
            
        } catch (Exception e) {
            // Publier un événement d'erreur
            publishEvent(new ErrorEvent(
                this,
                "Erreur lors du traitement du message de gestion",
                500,
                e
            ));
        }
    }
    
    @Override
    public boolean canHandle(MessageType type) {
        return type == MessageType.CREATE_USER 
            || type == MessageType.CREATE_GROUP
            || type == MessageType.UPDATE_PSEUDO;
    }
}
```

---

## Filtrage avancé

### Filtres prédéfinis réutilisables

```java
// Classe utilitaire pour filtres communs
public class EventFilters {
    
    // Filtre par conversation
    public static <T extends MessageEvent> EventFilter<T> byConversation(int conversationId) {
        return event -> event.getConversationId() == conversationId;
    }
    
    // Filtre par expéditeur
    public static <T extends MessageEvent> EventFilter<T> bySender(int senderId) {
        return event -> event.getMessage().getSenderId() == senderId;
    }
    
    // Filtre par contenu (contient une chaîne)
    public static EventFilter<TextMessageReceivedEvent> contentContains(String text) {
        return event -> event.getMessage().getContent().contains(text);
    }
    
    // Filtre par priorité minimale
    public static <T extends NotificationEvent> EventFilter<T> minPriority(NotificationPriority min) {
        return event -> event.getPriority().ordinal() >= min.ordinal();
    }
}

// Utilisation
EventSubscription<TextMessageReceivedEvent> sub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        this::handleMessage,
        EventFilters.byConversation(42),
        ExecutionMode.SYNC
    );
```

### Combinaison de filtres

```java
public class CombinedFilters {
    
    // ET logique
    public static <T extends Event> EventFilter<T> and(
            EventFilter<T> filter1, 
            EventFilter<T> filter2) {
        return event -> filter1.test(event) && filter2.test(event);
    }
    
    // OU logique
    public static <T extends Event> EventFilter<T> or(
            EventFilter<T> filter1, 
            EventFilter<T> filter2) {
        return event -> filter1.test(event) || filter2.test(event);
    }
    
    // NON logique
    public static <T extends Event> EventFilter<T> not(EventFilter<T> filter) {
        return event -> !filter.test(event);
    }
}

// Utilisation
EventFilter<TextMessageReceivedEvent> complexFilter = 
    CombinedFilters.and(
        EventFilters.byConversation(42),
        EventFilters.contentContains("important")
    );

EventSubscription<TextMessageReceivedEvent> sub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        this::handleImportantMessage,
        complexFilter,
        ExecutionMode.SYNC
    );
```

### Filtre avec état

```java
public class RateLimitFilter<T extends Event> implements EventFilter<T> {
    private final long minIntervalNanos;
    private long lastEventTime = 0;
    
    public RateLimitFilter(long minIntervalMillis) {
        this.minIntervalNanos = minIntervalMillis * 1_000_000;
    }
    
    @Override
    public boolean test(T event) {
        long now = System.nanoTime();
        long elapsed = now - lastEventTime;
        
        if (elapsed >= minIntervalNanos) {
            lastEventTime = now;
            return true;
        }
        return false;
    }
}

// Utilisation : limiter à 1 notification par seconde
EventSubscription<TextMessageReceivedEvent> sub = 
    controller.subscribeToEvent(
        TextMessageReceivedEvent.class,
        this::showNotification,
        new RateLimitFilter<>(1000), // 1 seconde
        ExecutionMode.ASYNC
    );
```

### Filtre avec cache

```java
public class BlockedUsersFilter implements EventFilter<MessageEvent> {
    private final Set<Integer> blockedUsers;
    private final ClientController controller;
    
    public BlockedUsersFilter(ClientController controller) {
        this.controller = controller;
        // Charger la liste des utilisateurs bloqués
        this.blockedUsers = loadBlockedUsers();
    }
    
    @Override
    public boolean test(MessageEvent event) {
        int senderId = event.getMessage().getSenderId();
        return !blockedUsers.contains(senderId);
    }
    
    public void blockUser(int userId) {
        blockedUsers.add(userId);
    }
    
    public void unblockUser(int userId) {
        blockedUsers.remove(userId);
    }
    
    private Set<Integer> loadBlockedUsers() {
        // Charger depuis la base de données ou configuration
        return new HashSet<>();
    }
}
```

---

## Bonnes pratiques

### 1. Toujours désabonner

```java
public class DialogController {
    private EventSubscription<TextMessageReceivedEvent> subscription;
    
    public void showDialog() {
        subscription = controller.subscribeToEvent(
            TextMessageReceivedEvent.class,
            event -> updateDialog(event)
        );
    }
    
    public void closeDialog() {
        if (subscription != null) {
            controller.unsubscribe(subscription);
            subscription = null;
        }
    }
}
```

### 2. Choisir le bon ExecutionMode

**SYNC** : Pour mises à jour rapides de l'UI
```java
controller.subscribeToEvent(
    TextMessageReceivedEvent.class,
    event -> {
        messageCountLabel.setText(String.valueOf(getMessageCount()));
    },
    ExecutionMode.SYNC
);
```

**ASYNC** : Pour traitements longs
```java
controller.subscribeToEvent(
    MediaMessageReceivedEvent.class,
    event -> {
        byte[] data = downloadMedia(event.getMessage().getMediaId());
        Image image = decodeImage(data);
    },
    ExecutionMode.ASYNC
);
```

### 3. Thread-safety avec Swing

```java
controller.subscribeToEvent(
    TextMessageReceivedEvent.class,
    event -> {
        // Si ExecutionMode.ASYNC, utiliser SwingUtilities.invokeLater()
        SwingUtilities.invokeLater(() -> {
            chatPanel.addMessage(event.getMessage());
        });
    },
    ExecutionMode.ASYNC
);
```

### 4. Utiliser des filtres simples et rapides

**❌ Éviter les filtres lourds** :
```java
// Mauvais : appel base de données dans le filtre
controller.subscribeToEvent(
    TextMessageReceivedEvent.class,
    this::handleMessage,
    event -> !database.isUserBlocked(event.getMessage().getSenderId()),
    ExecutionMode.SYNC
);
```

---

