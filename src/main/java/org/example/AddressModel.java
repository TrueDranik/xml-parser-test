package org.example;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressModel {
    int objectId;
    String name;
    List<AddressModel> children;
}
