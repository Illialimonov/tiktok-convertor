FROM python:3.11-bullseye

# Install Java separately
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    bash \
    curl \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Ensure pip is up-to-date
RUN python3 -m pip install --no-cache-dir --upgrade pip

# Install yt-dlp manually (standalone binary)
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

# Copy service account credentials



# Install required Python dependencies for yt-dlp
RUN python3 -m pip install --no-cache-dir requests curl_cffi

# Download and install the Google Cloud SDK manually
RUN curl https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.tar.gz > /tmp/google-cloud-sdk.tar.gz \
  && mkdir -p /usr/local/gcloud \
  && tar -C /usr/local/gcloud -xvf /tmp/google-cloud-sdk.tar.gz \
  && /usr/local/gcloud/google-cloud-sdk/install.sh --quiet

# Add Google Cloud SDK to the PATH
ENV PATH="$PATH:/usr/local/gcloud/google-cloud-sdk/bin"

COPY gcloud-key.json /app/gcloud-key.json

# Authenticate with Google Cloud inside the container
RUN gcloud auth activate-service-account --key-file=/app/gcloud-key.json

# Copy JAR file
COPY target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
