#!/bin/bash
#
# Script de lancement automatique du serveur et des clients Java.
# Compatible avec plusieurs terminaux et personnalisable via config.local.sh.
#

# === PARAMÈTRES DE BASE ===
CONFIG_FILE="./config.local.sh"
PROJECT_DIR="$(pwd)"
SERVER_CLASS="fr.uga.im2ag.m1info.chatservice.server.TchatsAppServer"
CLIENT_CLASS="fr.uga.im2ag.m1info.chatservice.client.Client"

# === CRÉATION DU FICHIER DE CONFIG SI INEXISTANT ===
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Fichier $CONFIG_FILE non trouvé, création avec des valeurs par défaut."

    # Liste de terminaux courants
    # Si vous utilisez un autre terminal, vous pouvez l'ajouter ici,
    # et l'ajouter également dans les fonctions de lancement plus bas.
    POSSIBLE_TERMINALS=("konsole" "gnome-terminal" "xfce4-terminal")

    # Tentative de détection automatique
    DETECTED_TERM=$(ps -o comm= -p $PPID | grep -E "konsole|gnome-terminal|xfce4-terminal" | head -n 1)

    echo "Terminals détectés possibles : ${POSSIBLE_TERMINALS[*]}"
    read -p "Entrez le terminal que vous utilisez [${DETECTED_TERM:-konsole} par défaut] : " USER_TERM

    if [ -z "$USER_TERM" ]; then
        if [ -n "$DETECTED_TERM" ]; then
            USER_TERM="$DETECTED_TERM"
            echo "Terminal détecté : $USER_TERM"
        else
            USER_TERM="konsole"
            echo "Aucun terminal détecté, utilisation du terminal par défaut : $USER_TERM"
        fi
    fi

    cat > "$CONFIG_FILE" <<EOF
# Configuration locale pour run_chat.sh
# Ce fichier n'est PAS versionné, chacun peut le modifier librement,
# sans impacter les autres utilisateurs.

TERMINAL="$USER_TERM"      # Terminal utilisé pour lancer les fenêtres
NUM_CLIENTS=2              # Nombre de clients par défaut
EOF

    echo "ichier de configuration créé : $CONFIG_FILE"
    echo "Vous pouvez le modifier à tout moment."
fi

# === CHARGEMENT DE LA CONFIGURATION ===
source "$CONFIG_FILE"

# === INTERPRÉTATION DES ARGUMENTS ===
MODE="all"
if [[ "$1" =~ ^[0-9]+$ ]]; then
    NUM_CLIENTS="$1"
elif [[ "$1" == "--server-only" ]]; then
    MODE="server"
elif [[ "$1" == "--clients-only" ]]; then
    MODE="clients"
elif [[ -n "$1" ]]; then
    echo "Argument non reconnu : $1"
    echo "Usage :"
    echo "  ./run_chat.sh [nombre_clients]"
    echo "  ./run_chat.sh --server-only"
    echo "  ./run_chat.sh --clients-only"
    exit 1
fi

echo "Configuration chargée :"
echo "   ➤ Terminal = $TERMINAL"
echo "   ➤ Clients  = $NUM_CLIENTS"
echo "   ➤ Mode     = $MODE"
echo

# === FONCTIONS ===

launch_server() {
    echo "Lancement du serveur..."
    case $TERMINAL in
        gnome-terminal)
            gnome-terminal --title="Serveur" -- bash -c "cd '$PROJECT_DIR' && mvn package exec:java -Dexec.mainClass='$SERVER_CLASS'; echo -e '\n[Serveur terminé]'; exec bash" &
            ;;
        konsole)
            konsole --noclose --new-tab --title "Serveur" -e bash -c "cd '$PROJECT_DIR' && mvn package exec:java -Dexec.mainClass='$SERVER_CLASS'; echo -e '\n[Serveur terminé]'; exec bash" &
            ;;
        xfce4-terminal)
            xfce4-terminal --title="Serveur" --hold -e "bash -c 'cd \"$PROJECT_DIR\" && mvn package exec:java -Dexec.mainClass=\"$SERVER_CLASS\"; exec bash'" &
            ;;
        *)
            echo "Terminal non supporté : $TERMINAL"
            exit 1
            ;;
    esac
}

launch_clients() {
    echo "Lancement de $NUM_CLIENTS clients..."
    for i in $(seq 1 $NUM_CLIENTS); do
        case $TERMINAL in
            gnome-terminal)
                gnome-terminal --title="Client $i" -- bash -c "cd '$PROJECT_DIR' && mvn package exec:java -Dexec.mainClass='$CLIENT_CLASS'; echo -e '\n[Client $i terminé]'; exec bash" &
                ;;
            konsole)
                konsole --noclose --new-tab --title "Client $i" -e bash -c "cd '$PROJECT_DIR' && mvn package exec:java -Dexec.mainClass='$CLIENT_CLASS'; echo -e '\n[Client $i terminé]'; exec bash" &
                ;;
            xfce4-terminal)
                xfce4-terminal --title="Client $i" --hold -e "bash -c 'cd \"$PROJECT_DIR\" && mvn package exec:java -Dexec.mainClass=\"$CLIENT_CLASS\"; exec bash'" &
                ;;
            *)
                echo "Terminal non supporté : $TERMINAL"
                exit 1
                ;;
        esac
        sleep 0.5
    done
}

# === MAIN ===
case $MODE in
    "server")
        launch_server
        ;;
    "clients")
        launch_clients
        ;;
    "all")
        launch_server
        sleep 2
        launch_clients
        ;;
esac

echo
echo "Tout est lancé !"
echo "   ➤ Mode : $MODE"
[[ $MODE != "server" ]] && echo "   ➤ Clients : $NUM_CLIENTS"
