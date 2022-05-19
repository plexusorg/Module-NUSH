package dev.plex.nush;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;

@Data
@AllArgsConstructor
public class Message
{

    UUID sender;
    Component message;
}
