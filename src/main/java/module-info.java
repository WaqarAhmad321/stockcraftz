module com.bigsteppers.stockcraftz {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires com.dbfx.framework;
    requires java.sql;
    requires bcrypt;
    requires org.json;

    opens com.bigsteppers.stockcraftz to javafx.fxml;
    exports com.bigsteppers.stockcraftz;

    opens com.bigsteppers.stockcraftz.controllers to javafx.fxml;
    exports com.bigsteppers.stockcraftz.controllers;

    opens com.bigsteppers.stockcraftz.utils to javafx.fxml;
    exports com.bigsteppers.stockcraftz.utils;
}