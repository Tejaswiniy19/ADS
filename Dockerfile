FROM openjdk:17-jdk-slim

# Install Python and pip
RUN apt-get update && apt-get install -y python3 python3-pip && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy all project files into the container
COPY . .

# Install Flask
RUN pip3 install --no-cache-dir flask

# Compile the Java benchmark application explicitly
RUN mkdir -p bin && javac -d bin -sourcepath src src/benchmark/SystemEvaluation.java src/utils/DataGenerator.java

# Run the Flask app on 0.0.0.0 so external networks can route to it
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV FLASK_RUN_PORT=5000

# Expose port (Render uses port 5000 by default or as configured)
EXPOSE 5000

# Start server
CMD ["python3", "-m", "flask", "run"]
