package com.gmail.andrewandy.customoregen.commands;

import com.gmail.andrewandy.corelib.api.command.NestedCommand;
import com.gmail.andrewandy.corelib.api.menu.ChestMenu;
import com.gmail.andrewandy.corelib.api.menu.Menu;
import org.bukkit.command.CommandSender;

public class CreateCommand extends NestedCommand {

    private static final CreateCommand instance = new CreateCommand();
    private Menu menu = new ChestMenu(45);

    private CreateCommand() {
        super("Create");
    }

    public static CreateCommand getInstance() {
        return instance;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, String[] args) {
        return true;
    }

}
