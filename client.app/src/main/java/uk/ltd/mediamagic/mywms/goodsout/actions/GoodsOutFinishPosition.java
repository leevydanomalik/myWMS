package uk.ltd.mediamagic.mywms.goodsout.actions;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.linogistix.los.inventory.facade.LOSGoodsOutFacade;
import de.linogistix.los.inventory.model.LOSGoodsOutRequestPosition;
import de.linogistix.los.inventory.query.LOSGoodsOutRequestPositionQueryRemote;
import javafx.application.Platform;
import uk.ltd.mediamagic.fx.data.TableKey;
import uk.ltd.mediamagic.fx.flow.Flow;
import uk.ltd.mediamagic.fx.flow.ViewContext;
import uk.ltd.mediamagic.fx.flow.actions.WithMultiSelection;


/** 
 * Sets the goods out request position to shipped.
 * It will send the unit load to the correct location and rename it if necessary.
 * @author slim
 *
 */
public class GoodsOutFinishPosition implements WithMultiSelection<Object> {
	
	@Override
	public void execute(Object source, Flow flow, ViewContext context, Collection<TableKey> key) {
		LOSGoodsOutFacade facade = context.getBean(LOSGoodsOutFacade.class);
		LOSGoodsOutRequestPositionQueryRemote query = context.getBean(LOSGoodsOutRequestPositionQueryRemote.class);
		List<Long> ids = key.stream().map(k -> (Long) k.get("id")).collect(Collectors.toList());		
		
		context.getExecutor().call(
				() -> {
					for (long id : ids) {
						LOSGoodsOutRequestPosition grp = query.queryById(id);	
						facade.finishPosition(grp.getSource().getLabelId(), grp.getGoodsOutRequest().getId());
					}
					return null;
				})
		.thenRunAsync(() -> flow.executeCommand(Flow.REFRESH_ACTION), Platform::runLater);
	}
	
}
