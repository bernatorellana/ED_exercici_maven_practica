package edu.meteo.app;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * "Appender" de Logback que envia cada missatge de log a la capsa de text de la finestra.
 * Es configura a logback.xml.
 */
public class AppenderAreaText extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** On s'han d'enviar els missatges. Mentre no hi ha finestra, és null i es descarten. */
    private static volatile Consumer<String> desti;

    public static void connecta(Consumer<String> nouDesti) {
        desti = nouDesti;
    }

    @Override
    protected void append(ILoggingEvent event) {
        Consumer<String> d = desti;
        if (d != null) {
            LocalTime hora = LocalTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
            d.accept(String.format("%s %-5s %s%n", HORA.format(hora), event.getLevel(), event.getFormattedMessage()));
        }
    }
}
