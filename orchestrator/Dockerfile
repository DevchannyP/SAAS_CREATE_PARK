FROM python:3.12-alpine
WORKDIR /app
COPY . .
RUN apk add --no-cache ffmpeg && adduser -D -H magazine && mkdir -p /output && chown magazine:magazine /output
USER magazine
EXPOSE 8090
CMD ["python","app.py"]
