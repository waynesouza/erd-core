package com.erd.core.util;

import java.util.regex.Pattern;

public class ConstantUtil {

    public static final String TEMPLATE_WELCOME = "welcome";
    public static final String TEMPLATE_RESET_PASSWORD = "reset-password";

    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

}
