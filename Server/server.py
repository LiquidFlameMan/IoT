import socket               # Import socket module

s = socket.socket()         # Create a socket object

local_ip = "192.168.88.237"

port = 12345                # Reserve a port for your service.
s.bind((local_ip, port))        # Bind to the port

s.listen(5)                 # Now wait for client connection.

f = open("test.csv", "w")
f.write("timestamp, longitude, latitude \n")
f.close()
print(local_ip, port)
while True:
   f = open("test.csv", "a")
   c, addr = s.accept()     # Establish connection with client.
   string = str(c.recv(1024))
   print ('Got connection from', addr)
   print (string[2:len(string)-1])
   f.write(string[2:len(string)-1])
   f.write("\n")
   c.close()                # Close the connection
   f.close()
