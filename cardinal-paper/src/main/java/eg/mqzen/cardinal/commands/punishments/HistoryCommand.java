package eg.mqzen.cardinal.commands.punishments;


import eg.mqzen.lib.commands.annotations.Command;
import eg.mqzen.lib.commands.annotations.Usage;
import eg.mqzen.lib.gui.base.pagination.PageComponent;
import eg.mqzen.lib.gui.base.pagination.Pagination;
import eg.mqzen.lib.gui.base.pagination.exception.InvalidPageException;
import eg.mqzen.cardinal.api.punishments.Punishment;
import eg.mqzen.cardinal.Cardinal;
import eg.mqzen.cardinal.punishments.gui.HistoryPage;
import eg.mqzen.cardinal.punishments.gui.PunishmentPageComponent;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Command("history")
public class HistoryCommand {


    @Usage
    public void def(Player source) {

        Cardinal.getInstance().getPunishmentManager()
                .getHistoryService()
                .getRecentPunishments(Duration.ofDays(365), 100)
                .onSuccess((punishmentsQueue)-> {

                    Pagination pagination = Pagination.auto(Cardinal.getInstance().getLotus())
                            .creator(new HistoryPage())
                            .componentProvider(()-> {
                                List<PageComponent> components = new ArrayList<>();
                                for (Punishment<?> punishment : punishmentsQueue) {
                                    components.add(new PunishmentPageComponent(punishment));
                                }
                                return components;
                            })
                            .build();

                    try {
                        pagination.open(source);
                    } catch (InvalidPageException ex) {
                        ex.printStackTrace();
                        //Pagination is empty or something else happened
                        source.sendMessage("There is no components or pages to display !!");
                    }

                });
    }


}
