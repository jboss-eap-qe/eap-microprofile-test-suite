package org.jboss.eap.qe.ts.common.server.config;

import org.wildfly.extras.creaper.commands.foundation.online.CliFile;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineCommand;
import org.wildfly.extras.creaper.core.online.OnlineCommandContext;

/**
 * Class that implements a command for Creaper to execute and CLI to add a datasource configuration.
 */
public class SetupDatasource implements OnlineCommand {
    @Override
    public void apply(OnlineCommandContext ctx) throws CommandFailedException {
        ctx.client.apply(new CliFile(SetupDatasource.class));
    }

    @Override
    public String toString() {
        return "SetupDatasource";
    }
}