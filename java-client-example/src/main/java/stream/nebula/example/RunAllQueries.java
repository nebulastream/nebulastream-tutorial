package stream.nebula.example;

import stream.nebula.exceptions.RESTException;

import java.io.IOException;

public class RunAllQueries {

    public static void main(String[] args) throws RESTException, IOException, InterruptedException {
        Query1.main(args);
        Query2.main(args);
        Query3.main(args);
        Query4.main(args);
        Query5.main(args);
        Query6.main(args);
        Query7.main(args);
        Query8.main(args);
        Query9.main(args);
    }

}
