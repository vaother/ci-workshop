FROM python:3.6

# The application's directory will be the working directory
WORKDIR /app

# Install dependencies defined in 'requirements.txt'
COPY src/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Define ENV
ENV PROJECTID USERID
ENV K8S_API_ENDPOINT K8S_API_SERVER

# Copy app's source code to the /app directory
COPY src /app

EXPOSE 5000

# Start the application
CMD ["python", "app.py"]
