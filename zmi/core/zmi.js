$(function() {
    function buildAllZones(data) {
        console.log("ZONES");
        console.log(data);
        var zones = $("<div />");
        for (var zoneName in data) {
            console.log(zoneName);
            zones.append($("<h2 />").text(zoneName));
            zones.append(buildAttributesTable(zoneName, data[zoneName]));
        }
        return zones;
    }

    function update() {
        $.post("/attributes/", function(data) {
            var table = buildAllZones(data);
            console.log(table);
            $("#zones").html(table);
            $("#attributes").html(table);
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    var myInterval = setInterval(update, 1000);

    update();

    $("#install-query-form").submit(function(e) {
        e.preventDefault();
        var name = $("#queryName").val();
        var code = $("#queryCode").val();
        $.post("/installQuery/", {"queryName": name, "query": code}, function(data) {
            location.reload();
        }).fail(function(xhr, status, error) {
            if (xhr.status == 400) {
                $("#maybeErrorInstallQuery").text(xhr.responseText);
            }
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    });

    function updateInstalledQueries() {
        $.get("/installedQueries/", function(data) {
            var queries = $("<table />");
            console.log("Installed");
            console.log(data);
            for (var q in data.values) {
                var row = $("<tr />");
                row.append($("<td />").text(q));
                row.append($("<td />").text(attributeValueToString(data.values[q])));
                var removeButton = $("<button class=\"remove-query-button\" type=\"button\">Remove</button>")
                    .data("queryName", q)
                    .click(function(e) {
                        console.log(e);
                        console.log(e.currentTarget);
                        var queryName = $(e.currentTarget).data("queryName");
                        console.log(queryName);
                        $.post("/uninstallQuery/", {"queryName": queryName}, function(data) {
                            location.reload();
                        }).fail(function(xhr, status, error) {
                            console.log(xhr);
                            console.log(status);
                            console.log(error);
                        });
                    });
                console.log(removeButton);
                row.append($("<td />").html(removeButton));
                queries.append(row);
            }
            $("#installed-query-list").html(queries);
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    updateInstalledQueries();

    var contacts = []

    function contactsFromData(data) {
        console.log(typeof(data));
        return JSON.parse(data).map(function(contact) { return {name: contact.name, address: contact.address}; });
    }

    function loadFallbackContacts() {
        var newContacts = [];
        $(".contact-input-set").each(function(i, contactData) {
            console.log("AAAA");
            console.log(contactData);
            var contact = {}
            $(contactData).children('input').each(function(i, d) {
                // FRAGILE
                console.log(d);
                if (i == 0) {
                    contact["name"] = d.value;
                } else if (i == 1) {
                    contact["address"] = d.value;
                }
            })
            console.log(contact);
            newContacts.push(contact);
        });
        contacts = newContacts;
    }

    function renderFallbackContacts() {
        var d = $("#fallback-contacts-inputs");
        d.html("");
        for (var i = 0; i < contacts.length; ++i) {
            var contact = $("<div class=\"contact-input-set\">");
            contact.append($("<input class=\"contact-name\">").val(contacts[i].name));
            contact.append($("<input class=\"contact-address\">").val(contacts[i].address));
            d.append(contact);
        }
    }

    function updateFallbackContacts() {
        $.get("/fallbackContacts/", function(data) {
            var queries = $("<table />");
            console.log("Fallback");
            console.log(data);
            contacts = contactsFromData(data);
            console.log(contacts);
            renderFallbackContacts();
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    updateFallbackContacts();

    $("#new-fallback-contact").click(function(e) {
        e.preventDefault();
        contacts.push({"name": "", "address": ""});
        renderFallbackContacts();
    });

    $("#fallback-contacts").submit(function(e) {
        console.log("!!!");
        e.preventDefault();
        loadFallbackContacts();
        console.log(contacts)
        $.post("/fallbackContacts/", JSON.stringify(contacts), function(data) {
        }).fail(function(xhr, status, error) {
            if (xhr.status == 400) {
                $("#maybeErrorFallbackContacts").text(xhr.responseText);
            }
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    })
});
