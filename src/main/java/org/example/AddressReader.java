package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddressReader {

    private static final String AS_ADDR_OBJ_FILE = "data/AS_ADDR_OBJ.XML";
    private static final String AS_ADM_HIERARCHY_FILE = "data/AS_ADM_HIERARCHY.XML";

    private static final String OBJECT = "OBJECT";
    private static final String ITEM = "ITEM";

    private static final String OBJECT_ID = "OBJECTID";
    private static final String NAME = "NAME";
    private static final String TYPE_NAME = "TYPENAME";
    private static final String START_DATE = "STARTDATE";
    private static final String END_DATE = "ENDDATE";
    private static final String IS_ACTUAL = "ISACTUAL";
    private static final String IS_ACTIVE = "ISACTIVE";
    private static final String PARENT_OBJ_ID = "PARENTOBJID";
    private static final String LEVEL = "LEVEL";

    public static void main(String[] args) {
        // решение задачи №1
        LocalDate targetDate = LocalDate.parse("2000-01-01");
        List<Integer> objectIds = Arrays.asList(1418203, 1422396, 1447339, 1449398, 1452841, 1453195);
        List<Address> addresses = getAddressDescriptions(objectIds, targetDate);
        printAddresses(addresses);

        System.out.println("====================================");

        // решение задачи №2
        List<String> addressesWithProezd = getAddressesWithProezd();
        printAddressesWithProezd(addressesWithProezd);
    }

    private static List<Address> getAddressDescriptions(List<Integer> objectIds, LocalDate targetDate) {
        List<Address> addresses = new ArrayList<>();

        try (InputStream asAddrObjStream = AddressReader.class.getClassLoader().getResourceAsStream(AS_ADDR_OBJ_FILE)) {
            var dbFactory = DocumentBuilderFactory.newInstance();
            var dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(asAddrObjStream);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName(OBJECT);
            for (int i = 0; i < nodeList.getLength(); i++) {
                var element = (Element) nodeList.item(i);
                int objectId = Integer.parseInt(element.getAttribute(OBJECT_ID));
                String name = element.getAttribute(NAME);
                String typeName = element.getAttribute(TYPE_NAME);
                LocalDate startDate = LocalDate.parse(element.getAttribute(START_DATE));
                LocalDate endDate = LocalDate.parse(element.getAttribute(END_DATE));
                boolean isActual = element.getAttribute(IS_ACTUAL).equals("1") ? Boolean.TRUE : Boolean.FALSE;
                boolean isActive = element.getAttribute(IS_ACTIVE).equals("1") ? Boolean.TRUE : Boolean.FALSE;

                if (objectIds.contains(objectId) && startDate.isAfter(targetDate) && endDate.isAfter(targetDate)
                        && isActual && isActive) {
                    Address address = Address.builder()
                            .objectId(objectId)
                            .name(name)
                            .typeName(typeName)
                            .build();
                    addresses.add(address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return addresses;
    }

    private static List<String> getAddressesWithProezd() {
        List<String> addressesWithProezd = new ArrayList<>();

        try (
                InputStream asAddrObjStream = AddressReader.class.getClassLoader().getResourceAsStream(AS_ADDR_OBJ_FILE);
                InputStream asAdmHierarchyStream = AddressReader.class.getClassLoader().getResourceAsStream(AS_ADM_HIERARCHY_FILE)
        ) {
            var dbFactory = DocumentBuilderFactory.newInstance();
            var dBuilder = dbFactory.newDocumentBuilder();
            Document docAddrObj = dBuilder.parse(asAddrObjStream);
            Document docAdmHierarchy = dBuilder.parse(asAdmHierarchyStream);
            docAddrObj.getDocumentElement().normalize();
            docAdmHierarchy.getDocumentElement().normalize();

            NodeList nodeListAddrObj = docAddrObj.getElementsByTagName(OBJECT);
            NodeList nodeListAdmHierarchy = docAdmHierarchy.getElementsByTagName(ITEM);

            Integer parentObjectIndex = getParentObjectIndex(nodeListAddrObj);

            var parentObject = (Element) nodeListAddrObj.item(parentObjectIndex);
            int objectId = Integer.parseInt(parentObject.getAttribute(OBJECT_ID));
            String name = parentObject.getAttribute(NAME);
            String typeName = parentObject.getAttribute(TYPE_NAME);

            AddressModel parentAddress = new AddressModel();
            parentAddress.setName(typeName + " " + name);
            parentAddress.setObjectId(objectId);

            List<AddressModel> childrenAddress = new ArrayList<>();
            List<Integer> childrenIndexes = getChildrenIndexes(nodeListAddrObj, nodeListAdmHierarchy, parentAddress.getObjectId());
            for (Integer childIndex : childrenIndexes) {
                AddressModel addressModel = getAddressModel(childIndex, nodeListAddrObj, nodeListAdmHierarchy);
                childrenAddress.add(addressModel);
            }
            parentAddress.setChildren(childrenAddress);
            printSpecificAddress(parentAddress, parentAddress.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return addressesWithProezd;
    }

    private static void printSpecificAddress(AddressModel addressModel, String addressName) {
        for (AddressModel child : addressModel.getChildren()) {
            String fullAddress = addressName + ", " + child.getName();
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                printSpecificAddress(child, fullAddress);
            }

            if (fullAddress.contains("проезд")) {
                System.out.println(fullAddress);
            }
        }
    }

    private static AddressModel getAddressModel(Integer index, NodeList nodeListAddrObj, NodeList nodeListAdmHierarchy) {
        var object = (Element) nodeListAddrObj.item(index);

        int objectId = Integer.parseInt(object.getAttribute(OBJECT_ID));
        String name = object.getAttribute(NAME);
        String typeName = object.getAttribute(TYPE_NAME);

        AddressModel parentAddress = new AddressModel();
        parentAddress.setObjectId(objectId);
        parentAddress.setName(typeName + " " + name);

        List<AddressModel> childrenAddress = new ArrayList<>();
        for (Integer childIndex : getChildrenIndexes(nodeListAddrObj, nodeListAdmHierarchy, parentAddress.getObjectId())) {
            AddressModel addressModel = getAddressModel(childIndex, nodeListAddrObj, nodeListAdmHierarchy);
            childrenAddress.add(addressModel);

        }
        parentAddress.setChildren(childrenAddress);
        return parentAddress;
    }

    private static Integer getParentObjectIndex(NodeList nodeListAddrObj) {
        Integer parentObjectIndex = null;
        for (int i = 0; i < nodeListAddrObj.getLength(); i++) {
            var elementAddrObj = (Element) nodeListAddrObj.item(i);
            if (elementAddrObj.getAttribute(LEVEL).equals("1") && isAddressActual(elementAddrObj)) {
                parentObjectIndex = i;
                break;
            }
        }
        return parentObjectIndex;
    }

    private static List<Integer> getChildrenIndexes(NodeList nodeListAddrObj, NodeList nodeListAdmHierarchy, Integer parentIndex) {
        List<Integer> indexes = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();

        for (int i = 0; i < nodeListAdmHierarchy.getLength(); i++) {
            var elementAddrObj = (Element) nodeListAdmHierarchy.item(i);
            boolean isParentObjId = elementAddrObj.getAttribute(PARENT_OBJ_ID).equals(String.valueOf(parentIndex));
            if (isParentObjId) {
                objectIds.add(elementAddrObj.getAttribute(OBJECT_ID));
            }
        }

        for (int i = 0; i < nodeListAddrObj.getLength(); i++) {
            var addressObject = (Element) nodeListAddrObj.item(i);
            boolean isAnyMatchObjectIds = objectIds.stream()
                    .anyMatch(it -> it.equals(addressObject.getAttribute(OBJECT_ID)));
            boolean addressActual = isAddressActual(addressObject);
            if (addressActual && isAnyMatchObjectIds) {
                indexes.add(i);
            }
        }

        return indexes;
    }

    private static boolean isAddressActual(Element elementAddrObj) {
        return elementAddrObj.getAttribute(IS_ACTUAL).equals("1");
    }

    private static void printAddresses(List<Address> addresses) {
        addresses.stream()
                .map(address -> address.getObjectId() + ": " + address.getTypeName() + " " + address.getName())
                .forEach(System.out::println);
    }

    private static void printAddressesWithProezd(List<String> addressesWithProezd) {
        addressesWithProezd.forEach(System.out::println);
    }
}

