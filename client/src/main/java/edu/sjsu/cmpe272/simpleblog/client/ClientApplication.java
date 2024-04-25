package edu.sjsu.cmpe272.simpleblog.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;



@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    private final IFactory factory;
    private final MicroblogCLI commands;

    public ClientApplication(IFactory factory, MicroblogCLI commands) {
        this.factory = factory;
        this.commands = commands;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
    @Override
    public void run(String... args) throws Exception {
        new CommandLine(commands, factory).execute(args);
    }

}