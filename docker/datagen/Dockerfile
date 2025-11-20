FROM python:3.11-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential       \
        gcc g++              \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN pip install --no-cache-dir --upgrade pip \
 && pip install --no-cache-dir \
        neurokit2 \
        medical_waveforms \
        paho-mqtt \
        PyYAML   \
        PyWavelets \
 && python -m pip cache purge   # keep the image tiny

 COPY datagen.py config.yml ./

 #CMD ["sh", "-c", "python datagen.py"]