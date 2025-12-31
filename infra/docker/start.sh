 #!/bin/bash
  cd ~/cardTransfo/infra/docker
  echo "🚀 Démarrage de la stack CardDemo..."
  echo ""
  docker compose up -d
  echo ""
  echo "⏳ Attente du démarrage des services (20 secondes)..."
  sleep 20
  echo ""
  echo "📊 État des services:"
  docker compose ps
  echo ""
  echo "✅ Stack démarrée avec succès!"
  echo ""
  echo "🌐 Accès:"
  echo "   Frontend:   http://13.37.173.179:3000"
  echo "   PostgreSQL: localhost:5432 (user: carddemo, pass: carddemo123)"
  echo "   Redis:      localhost:6379 (pass: carddemo123)"
  echo ""
  echo "📋 Commandes utiles:"
  echo "   docker compose logs -f        # Voir les logs en temps réel"
  echo "   docker compose ps             # État des services"
  echo "   docker compose down           # Arrêter la stack"
