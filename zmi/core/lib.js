function attributeValueToJson(attrValue) {
    if (attrValue.type == "ValueSet" || attrValue.type == "ValueList") {
        return attrValue.value.map(attributeValueToJson);
    }
    if (attrValue.type == "ValueContact") {
        return {"name": attrValue.name, address: attrValue.address}
    }
    return attrValue.value;
}

function attributeValueToString(attrValue) {
    return JSON.stringify(attributeValueToJson(attrValue));
}

function actionsForAttribute(zone, attrName, attrValue) {
    if (attrValue.type == "ValueInt" || attrValue.type == "ValueDouble" || attrValue == "ValueDuration") {
        var plotUrl = "/plot/" + attrName + "@" + zone;
        return $("<a href=\"" + plotUrl + "\">Plot</a>");
    }
}

function buildAttributesTable(zone, attrs) {
    var table = $("<table class=\"zmi-attributes\" />");
    for (var key in attrs.attributes.values) {
        value = attrs.attributes.values[key];
        var row = $("<tr />")
            .append(
                $("<td />").text(key)
            ).append(
                $("<td />").html(actionsForAttribute(zone, key, value))
            ).append(
                $("<td />").text(attributeValueToString(value))
            )
        table.append(row);
    }
    return table;
}