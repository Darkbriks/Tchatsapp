# Guide : Ajouter un nouveau type de message

## Étape 1: Ajouter le type dans l'enum si il n'existe pas

**Fichier**: `common/MessageType.java`

```java
public enum MessageType {
    TEXT,
    MEDIA,
    CREATE_USER,
    // ... autres types existants
    
    MON_NOUVEAU_TYPE,
    
    NONE;
}
```

**/!\ Attention:** Penser à modifier la méthode `toString()`.

## Étape 2: Créer la classe du message

**Fichier**: `common/messagefactory/MonNouveauMessage.java`

```java
package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.Packet;

public class MonNouveauMessage extends ProtocolMessage {
    
    // Attributs spécifiques
    private String monAttribut;
    
    /**
     * Constructeur par défaut OBLIGATOIRE pour le provider
     */
    public MonNouveauMessage() {
        super(MessageType.MON_NOUVEAU_TYPE, -1, -1);
    }
    
    // Getters/Setters éventuels
    public String getMonAttribut() {
        return monAttribut;
    }
    
    public void setMonAttribut(String monAttribut) {
        this.monAttribut = monAttribut;
    }
    
    @Override
    public Packet toPacket() {
        // Sérialiser vers Packet
        byte[] payload = monAttribut.getBytes();
        return new Packet.PacketBuilder(payload.length)
                .setMessageType(messageType)
                .setFrom(from)
                .setTo(to)
                .setPayload(payload)
                .build();
    }
    
    @Override
    public void fromPacket(Packet packet) {
        // Désérialiser depuis Packet
        this.messageType = packet.messageType();
        this.from = packet.from();
        this.to = packet.to();
        this.monAttribut = new String(packet.getModifiablePayload().array());
    }
}
```

## Étape 3: Créer le provider

**Fichier**: `common/messagefactory/MonNouveauMessageProvider.java`

```java
package fr.uga.im2ag.m1info.chatservice.common.messagefactory;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;

public class MonNouveauMessageProvider implements MessageProvider {
    
    @Override
    public MessageType getType() {
        return MessageType.MON_NOUVEAU_TYPE;
    }
    
    @Override
    public ProtocolMessage createInstance() {
        return new MonNouveauMessage();
    }
}
```

### ✅ Étape 4: Enregistrer dans META-INF

**Fichier**: `src/main/resources/META-INF/services/fr.uga.im2ag.m1info.chatservice.common.messagefactory.MessageProvider`

```
fr.uga.im2ag.m1info.chatservice.common.messagefactory.TextMessageProvider
fr.uga.im2ag.m1info.chatservice.common.messagefactory.ManagementMessageProvider
fr.uga.im2ag.m1info.chatservice.common.messagefactory.MediaMessageProvider
fr.uga.im2ag.m1info.chatservice.common.messagefactory.ReactionMessageProvider
fr.uga.im2ag.m1info.chatservice.common.messagefactory.NotificationMessageProvider
fr.uga.im2ag.m1info.chatservice.common.messagefactory.MonNouveauMessageProvider # Ajouter le chemin complet de la classe ici
```

## Étape 5: (Optionnel) Créer un handler

**Fichier**: `server/handlers/MonNouveauMessageHandler.java` ou équivalent client

```java
package fr.uga.im2ag.m1info.chatservice.server.handlers;

import fr.uga.im2ag.m1info.chatservice.common.MessageType;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.MonNouveauMessage;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ProtocolMessage;
import fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer;

public class MonNouveauMessageHandler extends ServerPacketHandler {
    
    @Override
    public void handle(ProtocolMessage message, TchatsAppServer.ServerContext serverContext) {
        if (!(message instanceof MonNouveauMessage msg)) {
            throw new IllegalArgumentException("Invalid message type");
        }
        
        System.out.println("Received: " + msg.getMonAttribut());
        
        // Forward ou autre logique
        serverContext.sendPacketToClient(msg.toPacket());
    }
    
    @Override
    public boolean canHandle(MessageType messageType) {
        return messageType == MessageType.MON_NOUVEAU_TYPE;
    }
}
```

## Étape 6: Enregistrer le handler

**Fichier**: `TchatsAppServer.java` ou équivalent client

```java
public static void main(String[] args) throws Exception {
    // ...
    ServerPacketRouter router = new ServerPacketRouter(s.serverContext);
    router.addHandler(new TextMessageHandler());
    router.addHandler(new MonNouveauMessageHandler()); // ← Ajouter ici
    s.setPacketProcessor(router);
    // ...
}
```


## Points importants

1. **Oublier le constructeur par défaut**
   ```java
   // Incorrect
   public MonNouveauMessage(String attr) { ... }
   
   // Correct
   public MonNouveauMessage() { ... }
   ```

2. **Oublier d'ajouter dans META-INF**
   - Sans cela, le ServiceLoader ne trouve pas le provider
   - Erreur: `IllegalArgumentException: Unknown message type`

3. **Format du fichier META-INF**
   - Une classe par ligne
   - Nom complet avec package
   - Pas de virgules, pas de point-virgules

## Ressources

- [Java ServiceLoader Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
- [Java ServiceLoader Explained](https://escuela-tech.medium.com/java-serviceloader-explained-4943c060d7f6)

---
