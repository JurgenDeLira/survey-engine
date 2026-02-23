package com.batteryplus.survey.connector.verina;

import com.batteryplus.survey.core.model.Customer;
import com.batteryplus.survey.core.model.PurchaseItem;
import org.springframework.jdbc.core.RowMapper;

//RowMapper(s)
public class VerinaRowMappers {

    public static final RowMapper<Customer> CUSTOMER = (rs, rowNum) -> new Customer(
            rs.getString("Nombre_Automovilista"),
            rs.getString("Telefono"),
            rs.getString("Email"),
            rs.getString("Marca_Carro"),
            rs.getString("Tipo"),
            rs.getString("Modelo"),
            rs.getTimestamp("Fecha_Garantia") != null
                    ? rs.getTimestamp("Fecha_Garantia").toLocalDateTime()
                    : null,
            rs.getObject("NoGarantia", Integer.class)
    );

    public static final RowMapper<PurchaseItem> PURCHASE_ITEM_ROW = (rs, rowNum) -> new PurchaseItem(
            rs.getString("Marca"),
            rs.getString("Producto")
    );

}
