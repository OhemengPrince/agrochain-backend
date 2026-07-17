package com.agrochain.backend.constants;

import java.util.LinkedHashMap;
import java.util.Map;

// Bank name -> Paystack Ghana bank code, used for the bank-account withdrawal
// path (transfer recipient creation and /bank/resolve verification).
public final class GhanaBanksConstants {

    private GhanaBanksConstants() {
    }

    public static final Map<String, String> BANKS = new LinkedHashMap<>();

    // Verified live against GET https://api.paystack.co/bank?country=ghana&currency=GHS
    // — Paystack's actual Ghana bank codes are 6-digit GHIPSS codes, not the
    // 3-digit codes originally given in the task spec (which don't resolve at
    // all: e.g. "040" 404s with "Unknown bank code", confirmed by a live call).
    static {
        BANKS.put("GCB Bank", "040100");
        BANKS.put("Ecobank Ghana", "130100");
        BANKS.put("Absa Bank Ghana", "030100");
        BANKS.put("Fidelity Bank Ghana", "240100");
        BANKS.put("Stanbic Bank Ghana", "190100");
        BANKS.put("Access Bank Ghana", "280100");
        BANKS.put("Zenith Bank Ghana", "120100");
        BANKS.put("CAL Bank", "140100");
        BANKS.put("UBA Ghana", "060100");
        BANKS.put("Republic Bank Ghana", "110100");
        BANKS.put("Agricultural Development Bank", "080100");
        BANKS.put("National Investment Bank", "050100");
        BANKS.put("Prudential Bank", "180100");
        BANKS.put("First Atlantic Bank", "170100");
        BANKS.put("GT Bank Ghana", "230100");
        BANKS.put("Bank of Africa Ghana", "210100");
        BANKS.put("Consolidated Bank Ghana", "340100");
        BANKS.put("OmniBank Ghana", "360100");
    }
}
