FROM python:3.12-alpine
WORKDIR /app
COPY . .
RUN adduser -D -H magazine
USER magazine
EXPOSE 8090
CMD ["python","app.py"]
