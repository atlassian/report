/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package tools.profiler.jfr.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JfrClass extends Element {
    public final int id;
    public final boolean simpleType;
    public final String name;
    public final List<JfrField> fields;

    public JfrClass(Map<String, String> attributes) {
        this.id = Integer.parseInt(attributes.get("id"));
        this.simpleType = "true".equals(attributes.get("simpleType"));
        this.name = attributes.get("name");
        this.fields = new ArrayList<>(2);
    }

    @Override
    public void addChild(Element e) {
        if (e instanceof JfrField) {
            fields.add((JfrField) e);
        }
    }

    JfrField field(String name) {
        for (JfrField field : fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }
        return null;
    }
}
