web: gunicorn hello_kuppam.wsgi:application --workers 2 --threads 4 --worker-class gthread --timeout 30 --graceful-timeout 30 --max-requests 500 --max-requests-jitter 50 --preload --log-file -
release: python manage.py migrate --noinput
