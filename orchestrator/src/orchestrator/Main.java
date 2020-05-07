package orchestrator;

import java.io.Console;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args){
        String username, password;
        List<Integer> networks, ports;
        List<String> aliases = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        Console con = System.console();

// USE WHEN TESTING COMPLETE

//        if(con == null){
//            System.out.println("No console available");
//            return;
//        }

try {
    if (args[0].equals("--help") || args[0].equals("-h")) {
        System.out.println("Usage: relay {COMMAND | --help | -h}");
        System.out.println("              UPDATE network_list... TO port_list... WITH [alias_list...]");
        System.out.println("                    update a comma separated list of networks to the comma separated list of ports with an optional comma separated list of aliases");
        System.out.println("              DISPLAY");
        System.out.println("                    display all networks with their respective ports and aliases");
        System.out.println("              ADD port_list... WITH [alias_list...]");
        System.out.println("                    add with auto incremented network ids a comma separated list of their ports with an optional comma separated list of aliases");
        System.out.println("              DELETE network_list...");
        System.out.println("                    delete a comma separated list of networks");
        System.out.println("              START");
        System.out.println("                    start the relay");

        return;
    } else if (args[0].equals("UPDATE")) {

        String[] tempNets, tempPorts, tempAliases;


            if(args.length == 4 && args[2].equals("TO"))
                ;
            else if (args.length == 6 && args[2].equals("TO") &&  args[4].equals("WITH")){
                tempAliases = args[5].split(",");
                aliases = Arrays.asList(tempAliases);
            } else {
                System.out.println("Syntax invalid, try 'relay --help'");
                return;
            }

        tempNets = args[1].split(",");
        tempPorts = args[3].split(",");

        networks = Arrays.stream(tempNets).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
        ports = Arrays.stream(tempPorts).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

        System.out.print("Enter username: ");
        username = scanner.nextLine();
        System.out.print("Enter password: ");
        password = scanner.nextLine();
//        password = new String(con.readPassword("Enter password: "));




//        for(int i = 0; i< networks.size(); i++){
//            System.out.println("network:" + networks.get(i) + " ports:" + ports.get(i) + " aliases: "+ aliases.get(i));
//            System.out.println(username + " " + password);
//        }

    } else if (args[0].equals("DISPLAY")) {

    } else if (args[0].equals("ADD")) {

    } else if (args[0].equals("DELETE")) {

    } else if (args[0].equals("START")) {

    }
}catch(Exception e){}
//        port = Integer.parseInt(args[0]);
//        username = args[1];
//        System.out.println();
//        System.out.print("Enter password: ");
//        password = new String(con.readPassword());

    }
}
