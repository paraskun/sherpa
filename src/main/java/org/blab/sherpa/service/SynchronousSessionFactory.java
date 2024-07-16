package org.blab.sherpa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

@Service
public class SynchronousSessionFactory implements SessionFactory {
  private final InetSocketAddress address;

  public SynchronousSessionFactory(
      @Value("${address.hostname}") String hostname, @Value("${address.port}") Integer port) {
    this.address = new InetSocketAddress(hostname, port);
  }

  @Override
  public Session createSession() {
    return new SynchronousSession(address);
  }
}
