package org.jamesdbloom.mockserver.client.serialization.model;

import org.jamesdbloom.mockserver.model.KeyToMultiValue;
import org.jamesdbloom.mockserver.model.ModelObject;

import java.util.List;

/**
 * @author jamesdbloom
 */
public class KeyToMultiValueDTO<K, V> extends ModelObject {
    private K name;
    private List<V> values;

    protected KeyToMultiValueDTO(KeyToMultiValue<K, V> keyToMultiValue) {
        name = keyToMultiValue.getName();
        values = keyToMultiValue.getValues();
    }

    protected KeyToMultiValueDTO() {
    }

    public K getName() {
        return name;
    }

    public KeyToMultiValueDTO setName(K name) {
        this.name = name;
        return this;
    }

    public List<V> getValues() {
        return values;
    }

    public KeyToMultiValueDTO setValues(List<V> values) {
        this.values = values;
        return this;
    }
}
