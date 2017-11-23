function attributeValueToString(attrValue) {
    return JSON.stringify(attrValue);
}

function buildAttributesTable(attrs) {
    var table = $("<table class=\"zmi-attributes\" />");
    console.log(attrs);
    console.log(attrs.attributes.values);
    for (var key in attrs.attributes.values) {
        console.log(key);
        value = attrs.attributes.values[key];
        var row = $("<tr />")
            .append(
                $("<td />").text(key)
            ).append(
                $("<td />").text(attributeValueToString(value))
            )
        console.log(row);
        table.append(row);
    }
    return table;
}