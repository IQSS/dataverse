package edu.harvard.iq.dataverse.api.helpers;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(value = DBSettings.class)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DBSettingExtension.class)
public @interface DBSetting {
    SettingsServiceBean.Key name();
    String value();
}
