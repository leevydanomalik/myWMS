package uk.ltd.mediamagic.flow.crud;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.mywms.model.BasicEntity;

import de.linogistix.los.crud.BusinessObjectCRUDRemote;
import de.linogistix.los.entityservice.BusinessObjectLockState;
import de.linogistix.los.query.BODTO;
import de.linogistix.los.query.BusinessObjectQueryRemote;
import de.linogistix.los.query.QueryDetail;
import de.linogistix.los.query.TemplateQuery;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import uk.ltd.mediamagic.annot.Worker;
import uk.ltd.mediamagic.fx.ApplicationPane;
import uk.ltd.mediamagic.fx.FxExceptions;
import uk.ltd.mediamagic.fx.FxMainMenuPlugin;
import uk.ltd.mediamagic.fx.MFXMLLoader;
import uk.ltd.mediamagic.fx.converters.ToStringConverter;
import uk.ltd.mediamagic.fx.data.TableKey;
import uk.ltd.mediamagic.fx.flow.ApplicationContext;
import uk.ltd.mediamagic.fx.flow.BasicFlow;
import uk.ltd.mediamagic.fx.flow.ContextBase;
import uk.ltd.mediamagic.fx.flow.FXErrors;
import uk.ltd.mediamagic.fx.flow.Flow;
import uk.ltd.mediamagic.fx.flow.FlowContext;
import uk.ltd.mediamagic.fx.flow.ViewContext;
import uk.ltd.mediamagic.fx.flow.ViewContextBase;
import uk.ltd.mediamagic.fxcommon.UserPermissions;
import uk.ltd.mediamagic.mywms.BeanDirectory;
import uk.ltd.mediamagic.mywms.FlowUtils;
import uk.ltd.mediamagic.mywms.common.BeanUtils;
import uk.ltd.mediamagic.mywms.common.Editor;
import uk.ltd.mediamagic.mywms.common.LockStateConverter;
import uk.ltd.mediamagic.mywms.common.MyWMSUserPermissions;
import uk.ltd.mediamagic.mywms.common.TableColumnBinding;

public abstract class CRUDPlugin<T extends BasicEntity> extends FxMainMenuPlugin implements Editor<T> {
	private final Class<? extends BusinessObjectQueryRemote<T>> queryBean;
	private final Class<? extends BusinessObjectCRUDRemote<T>> crudBean;
	private final Class<T> boClass;
	private final BeanInfo beanInfo;
	private UserPermissions userPermissions = new MyWMSUserPermissions();

	public CRUDPlugin(Class<T> boClass) {
		super();
		this.queryBean = BeanDirectory.getQuery(boClass);
		this.crudBean = BeanDirectory.getCRUD(boClass);
		this.boClass = boClass;
		this.beanInfo = BeanUtils.getBeanInfo(boClass);
	}
	
	protected BeanInfo getBeanInfo() {
		return beanInfo;
	}

	protected Class<T> getBoClass() {
		return boClass;
	}

	protected Class<? extends BusinessObjectQueryRemote<T>> getQueryClass() {
		return queryBean;
	}

	protected Class<? extends BusinessObjectCRUDRemote<T>> getCRUDClass() {
		return crudBean;
	}

	public UserPermissions getUserPermissions() {
		return userPermissions;
	}

	protected void setUserPermissions(UserPermissions userPermissions) {
		this.userPermissions = userPermissions;
	}
	
	@Override
	public void edit(ContextBase context, Class<T> dataClass, long id) {
		ApplicationContext appContext = context.getBean(ApplicationContext.class);
		
		Flow flow = createNewFlow(appContext);
		flow.start(MyWMSEditor.class, c -> getEditor(c, new TableKey("id", id)));
		FlowUtils.startNewFlow(appContext, flow);
	}

	@Override
	public void view(ContextBase context, Flow flow, Class<T> dataClass, long id) {
		MyWMSEditor<T> editor = getEditor(context, new TableKey("id", id));
		editor.getCommands().clear();
		editor.setUserPermissions(new UserPermissions.ReadOnly());
		FlowUtils.showPopup(dataClass.getSimpleName(), context, editor);
	}

	@Override
	public Callback<ListView<BODTO<T>>, ListCell<BODTO<T>>> createTOListCellFactory() {
		return null;
	}
	
	@Override
	public Callback<ListView<T>, ListCell<T>> createListCellFactory() {
		return TextFieldListCell.forListView(new ToStringConverter<>(BasicEntity::toUniqueString));
	}
	
	protected StringConverter<?> getConverter(PropertyDescriptor property) {
		if ("lock".equals(property.getName())) {
			return new LockStateConverter<>(BusinessObjectLockState.class);
		}
		return null;
	}
	
	@Override
	protected BooleanBinding createDisableBinding() {
		return Bindings.createBooleanBinding(() -> false);
	}
	
	@Override
	protected BooleanBinding createVisibleBinding() {
		return Bindings.createBooleanBinding(() -> true);
	}
	
	@Worker void save(ContextBase context, T data) throws Exception {
		System.out.println("SAVE 2");
		BusinessObjectCRUDRemote<T> query = context.getBean(crudBean);
		query.update(data);
	}

	@Worker void delete(ContextBase context, T data) throws Exception {
		BusinessObjectCRUDRemote<T> query = context.getBean(crudBean);
		query.delete(data);
	}

	@Worker
	public List<T> getListData(ContextBase context,  QueryDetail detail, TemplateQuery template) throws Exception {
		BusinessObjectQueryRemote<T> query = context.getBean(queryBean);
		template.setBoClass(boClass);
		List<T> list = query.queryByTemplate(detail,template);
		return list;
	}
	
	@Worker 
	public T getData(ContextBase context, long id) throws Exception {
		BusinessObjectQueryRemote<T> query = context.getBean(queryBean);
		T obj = query.queryById(id);
		return obj;
	}
	
	protected	void save(PoJoEditor<T> source, Flow flow, ViewContext context) {
		T data = source.getData();
		source.setData(null);
		context.getExecutor().call(() -> {
			save(context, data);
			return getData(context, data.getId());
		})
		.whenCompleteUI((d,e) -> {
			if (e != null) {
				source.setData(data);
				FxExceptions.exceptionThrown(e);
			}
			else if (d != null) {
				source.setData(d);
			}
			else {
				FXErrors.error("Data load error", "Count not refresh data after save. ID:" + data.getId());
			}
		});
	}

	protected void refresh(PoJoEditor<T> source, Flow flow, ViewContext context) {
		T data = source.getData();
		source.setData(null);
		context.getExecutor().call(() -> {
			return getData(context, data.getId());
		})
		.whenCompleteUI((d,e) -> {
			if (e != null) {
				FxExceptions.exceptionThrown(e);
			}
			else if (d != null) {
				source.setData(d);
			}
			else {
				FXErrors.error("Data load error", "Count not refresh data after save. ID:" + data.getId());
			}
		});
	}

	protected	void refresh(CrudTable<T> source, ViewContextBase context) {
		source.setItems(null);
		source.getExecutor().call(() -> getListData(context, source.queryDetailProperty().get(), source.queryTemplateProperty().get()))
		.thenApplyUI(FXCollections::observableList)
		.thenAccept(source::setItems);			
	}

	@SuppressWarnings("unchecked")
	public Flow createNewFlow(ApplicationContext context) {
		BasicFlow flow = new BasicFlow(new FlowContext(context));		
		String title = getName(getPath());
		flow.setTitle(title);
		flow
		.global()
			.back()
			.alias(Flow.CANCEL_ACTION, Flow.BACK_ACTION)
		.end()
		.with(CrudTable.class)
			.withSelection(Flow.EDIT_ACTION, this::getEditor)
			.alias(Flow.TABLE_SELECT_ACTION, Flow.EDIT_ACTION)
			.action(Flow.REFRESH_ACTION, (s,f,c) -> this.refresh((CrudTable<T>)s, c))
		.end()
		.with(MyWMSEditor.class)
			.action(Flow.SAVE_ACTION, this::save)
			.action(Flow.REFRESH_ACTION, (s,f,c) -> this.refresh((PoJoEditor<T>)s,f,c))
		.end();		
		return flow;
	}
	
	public Flow createFlow(ApplicationContext context , Scene parent, Pane root) {
		Flow flow = createNewFlow(context);
		flow.start(CrudTable.class, root, (c) -> getTable(c));
		return flow;
	}
	
	protected MyWMSEditor<T> getEditor(ContextBase context, TableKey key) {
		Long id = key.get("id");
		if (id == null) return null;

		URL url = getClass().getResource(boClass.getSimpleName() + ".fxml");

		MyWMSEditor<T> controller = new MyWMSEditor<>(getBeanInfo(), this::getConverter);
		context.autoInjectBean(controller);
		if (url == null) {
			SubForm[] subForms = getClass().getAnnotationsByType(SubForm.class);
			List<SubForm> subFormsList = (subForms == null) ? Collections.emptyList() : Arrays.asList(subForms);
			PojoForm form = new MyWMSForm(getBeanInfo(), subFormsList);
			form.bindController(controller);
		}
		else {
			MFXMLLoader.loadFX(url, controller);
		}
		controller.getExecutor().call(() -> getData(context, id))
		.thenSetUI(controller.dataProperty());
		return controller;
	}
	
	
	
	protected void getEditor(CrudTable<T> source, Flow flow, ViewContext context, TableKey key) {
		MyWMSEditor<T> controller = getEditor(context, key);
		controller.setUserPermissions(getUserPermissions());
		context.setActiveBean(MyWMSEditor.class, controller);
		flow.next(context);
	}
	
	/**
	 * A list of property names for display in the table view.
	 * This is used by the <code>getTable()</code> method.
	 * @return a list of property names.
	 */
	protected abstract List<String> getTableColumns();

	/**
	 * Generate the table layout for table selectors.
	 * The method should be overridden when the table layout needs to be customised.
	 * @param context the context for preparing the controller.
	 * @return the table layout.
	 */
	protected CrudTable<T> getTable(ViewContextBase context) {
		CrudTable<T> table = new CrudTable<>();	
		
		UserPermissions userPermissions = getUserPermissions();
		Iterable<String> columns = getTableColumns().stream()
				.filter(s -> Optional.ofNullable(userPermissions.isVisible(s)).map(ObservableBooleanValue::get).orElse(true))
				::iterator;

		TableColumnBinding<T> tcb = new TableColumnBinding<>(getBeanInfo(),	columns, "id"::equals);
		tcb.setConverterFactory(this::getConverter);
		Bindings.bindContent(table.getTable().getColumns(), tcb);
		table.getCommands()
			.cru()
		.end();
		
		context.autoInjectBean(table);
		Runnable refreshData = () -> refresh(table, context);
		
		refreshData.run();
		
		table.queryDetailProperty().addListener(o -> refreshData.run());
		table.queryTemplateProperty().addListener(o -> refreshData.run());
		
		return table;
	}
		
	@Override
	public void handle(ApplicationContext context, Parent source, Function<Node, Runnable> showNode) {
		AnchorPane parent = new AnchorPane();
		parent.getChildren().add(new Label("Waiting..."));
		Flow flow = createFlow(context, source.getScene(), parent);
		if (flow == null) return; // opperation canceled

		flow.setOnDisplayNode(showNode);
				
		configureFlowNode(parent, flow);

		Runnable onClose = showNode.apply(parent);
		flow.setOnClose(onClose);
		flow.executeStartAction(parent);
	}

	public static Pane configureFlowNode(Pane parent, Flow flow) {		
		EventHandler<Event> onCloseRequestHandler = flow.createOnCloseRequestHandler();		
		ApplicationPane.getTitleProperty(parent).bind(flow.titleProperty());
		ApplicationPane.setOnCloseRequestHandler(parent, onCloseRequestHandler);
		return parent;
	}
}
