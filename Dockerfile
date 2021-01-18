FROM python:3.6

ENV PROJECTID USERID

# The application's directory will be the working directory
WORKDIR /app

# Install dependencies defined in 'requirements.txt'
COPY src/requirements.txt /tmp/requirements.txt
RUN pip install -r /tmp/requirements.txt

# Copy app's source code to the /app directory
COPY src /app

EXPOSE 5000

# Start the application
CMD ["python", "app.py"]
