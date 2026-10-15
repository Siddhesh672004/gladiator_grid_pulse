package com.gridpulse.gateway.ratelimit;

// who a bucket belongs to: IP = always the address (login forms), CLIENT = the logged-in user, else the address
public enum KeyType {
    IP,
    CLIENT
}
