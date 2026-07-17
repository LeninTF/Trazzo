FROM python:3.13-slim

WORKDIR /app

# deps de sistema para OpenCV headless + ONNX Runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libgl1 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# opencv-python no tiene GUI en el servidor; headless es más liviano
COPY requirements.txt .
RUN sed 's/opencv-python==/opencv-python-headless==/g' requirements.txt > requirements_docker.txt \
    && pip install --no-cache-dir -r requirements_docker.txt \
    && rm requirements_docker.txt requirements.txt

COPY src/ ./src/

ENV PYTHONPATH=/app/src
ENV PYTHONUNBUFFERED=1

# en Linux la cámara usa v4l2, no dshow
ENV CAMERA_BACKEND=v4l2
# escuchar en todas las interfaces para que Docker exponga el puerto
ENV WEBSOCKET_HOST=0.0.0.0

EXPOSE 9002

CMD ["python", "src/server.py"]
