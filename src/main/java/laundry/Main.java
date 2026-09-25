package laundry;

import javax.swing.SwingUtilities;
import laundry.gui.LaundryGui;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
            Main.class
        )
            .web(WebApplicationType.NONE)
            .headless(false)
            .run(args);

        SwingUtilities.invokeLater(() ->
            context.getBean(LaundryGui.class).display()
        );
    }
}
