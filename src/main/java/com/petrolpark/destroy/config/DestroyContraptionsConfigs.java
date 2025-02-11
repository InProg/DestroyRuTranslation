package com.petrolpark.destroy.config;

public class DestroyContraptionsConfigs extends DestroyConfigBase {

    public final ConfigInt maxChainLength = i(10, "maxChainLength", Comments.maxChainLength);

    public final ConfigGroup dynamo = group(0, "dynamo", Comments.dynamo);
    public final ConfigBool dynamoBulkCharging = b(true, "dynamoBulkCharging", Comments.dynamoBulkCharging);

    public final ConfigGroup vat = group(0, "vat", Comments.vat);
    public final ConfigBool vatExplodesAtHighPressure = b(true, "vatExplodesAtHighPressure", Comments.vatExplodesAtHighPressure);
    
    
    @Override
    public String getName() {
        return "contraptions";
    };

    private static class Comments {

        static String
        maxChainLength = "Maximum length of a chain connecting two Cogwheels", 
        dynamo = "Dynamo",
        dynamoBulkCharging = "Whether Dynamos can charge multiple Item Stacks at once.",
        vat = "Vat",
        vatExplodesAtHighPressure = "Whether Vats explode if the pressure exceeds the maximum of the weakest block.";
    };
}
