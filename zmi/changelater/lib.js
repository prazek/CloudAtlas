function attributeValueToString(attrValue) {
    return JSON.stringify(attrValue);
}

function buildAttributesTable(attrs) {
    var table = $("<table />");
    console.log(attrs);
    console.log(attrs.attributes.map);
    for (var key in attrs.attributes.map) {
        console.log(key);
        value = attrs.attributes.map[key];
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