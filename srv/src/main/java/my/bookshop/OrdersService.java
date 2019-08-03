package my.bookshop;

import java.util.ArrayList;
import java.util.List;

import com.sap.cloud.sdk.service.prov.api.*;
import com.sap.cloud.sdk.service.prov.api.annotations.*;
import com.sap.cloud.sdk.service.prov.api.exits.*;
import com.sap.cloud.sdk.service.prov.api.request.*;
import com.sap.cloud.sdk.service.prov.api.response.*;
import org.slf4j.*;

import java.util.UUID;
import java.util.Date;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import com.sap.demo.bookshop.jpa.my.bookshop.Books;
import com.sap.demo.bookshop.jpa.my.bookshop.Orders;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import com.sap.cloud.sdk.service.prov.api.operations.*;

public class OrdersService {

	private static final Logger LOG = LoggerFactory.getLogger(OrdersService.class.getName());

	@BeforeRead(entity = "Orders", serviceName = "CatalogService")
	public BeforeReadResponse beforeReadOrders(ReadRequest req, ExtensionHelper h) {
		LOG.error("##### Orders - beforeReadOrders ########");
		return BeforeReadResponse.setSuccess().response();
	}

	@AfterRead(entity = "Orders", serviceName = "CatalogService")
	public ReadResponse afterReadOrders(ReadRequest req, ReadResponseAccessor res, ExtensionHelper h) {
		EntityData ed = res.getEntityData();
		EntityData ex = EntityData.getBuilder(ed).addElement("amount", 1000).buildEntityData("Orders");
		return ReadResponse.setSuccess().setData(ex).response();
	}

	@AfterQuery(entity = "Orders", serviceName = "CatalogService")
	public QueryResponse afterQueryOrders(QueryRequest req, QueryResponseAccessor res, ExtensionHelper h) {
		List<EntityData> dataList = res.getEntityDataList(); // original list
		List<EntityData> modifiedList = new ArrayList<EntityData>(dataList.size()); // modified list
		for (EntityData ed : dataList) {
			EntityData ex = EntityData.getBuilder(ed).addElement("amount", 1000).buildEntityData("Orders");
			modifiedList.add(ex);
		}
		return QueryResponse.setSuccess().setData(modifiedList).response();
	}

	@Create(entity = "Orders", serviceName = "CatalogService")
	public CreateResponse createOrder(CreateRequest createRequest, ExtensionHelper extensionHelper)
			throws NamingException, ODataApplicationException {

		// First, we fetch an EntityManager from JNDI context to perform our JPA
		// operations with.
		EntityManager em = (EntityManager) (new InitialContext()).lookup("java:comp/env/jpa/default/pc");

		// Next, we create an Orders object from the data sent with the Create request.
		// We set an ID for the order, its creation time, an order amount of 1000 units,
		// and set the object reference to the ordered book. All other attributes of the
		// Orders entity remain unchanged (for example, the buyer).
		Orders order = createRequest.getData().as(Orders.class);
		order.setID(UUID.randomUUID().toString());
		order.setDate(new Date());
		order.setAmount(1000);
		order.setBook(em.find(Books.class, createRequest.getData().getElementValue("book_ID")));

		// Using the persist method of the EntityManager we add the newly created
		// Orders object to the JPA persistency, so that once the current transaction is
		// committed, this new order is added to the database.
		em.persist(order);

		// Finally, to return the newly created entity in the response, we create an
		// EntityData representation of the order.
		EntityData createdEntity = EntityData.getBuilder(createRequest.getData()).addElement("ID", order.getID())
				.addElement("book", order.getBook()).addElement("buyer", order.getBuyer())
				.addElement("date", order.getDate()).addElement("amount", order.getAmount()).buildEntityData("Orders");

		return CreateResponse.setSuccess().setData(createdEntity).response();
	}
}
