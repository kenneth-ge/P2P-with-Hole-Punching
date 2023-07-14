from threading import Thread, Event

import socket
import time

UDP_IP = "localhost"
UDP_PORT = 5000

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

sock.sendto(b"add Kenny2\0", (UDP_IP, UDP_PORT))

chat_with = None

while True:
    data, addr = sock.recvfrom(1024)

    num_ppl = data[0]

    print(num_ppl, 'people found')

    people = []

    pos = 1
    for i in range(num_ppl):
        addr_len = data[pos]
        pos += 1
        addr = data[pos:pos+addr_len]
        pos += addr_len
        port = 0
        port += 0xFF & data[pos]
        pos += 1
        port <<= 8
        port += 0xFF & data[pos]
        pos += 1
        port <<= 8
        port += 0xFF & data[pos]
        pos += 1
        port <<= 8
        port += 0xFF & data[pos]
        pos += 1

        namelen = data[pos]
        pos += 1

        name = data[pos:pos+namelen]
        pos += namelen

        people.append((name, addr, port))

        print('person', i, ':', name, addr, port)

    person = int(input('enter the id number of the person, or -1 if you want to see if more devices are available'))

    if person == -1:
        sock.sendto(b"add Kenny2\0", (UDP_IP, UDP_PORT))
        continue
    else:
        chat_with = people[person]
        break

name, addr, port = chat_with
addr = ('.'.join(str(c) for c in addr))

print('chatting with', name, 'at', addr, port)

def listen():
    while True:
        data, addr = sock.recvfrom(1024)
        print(data)

listening = Thread(target=listen)
listening.start()

while True:
    msg = input()
    sock.sendto(bytes(msg, 'utf-8'), (addr, port))