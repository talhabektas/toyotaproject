function calculate(dependencies) {

    const pf1GbpUsd = dependencies["PF1_GBPUSD"];
    const pf2GbpUsd = dependencies["PF2_GBPUSD"];

    const pf1UsdTry = dependencies["PF1_USDTRY"];
    const pf2UsdTry = dependencies["PF2_USDTRY"];

    const gbpUsdBid = (pf1GbpUsd.bid + pf2GbpUsd.bid) / 2;
    const gbpUsdAsk = (pf1GbpUsd.ask + pf2GbpUsd.ask) / 2;
    const usdTryBid = (pf1UsdTry.bid + pf2UsdTry.bid) / 2;
    const usdTryAsk = (pf1UsdTry.ask + pf2UsdTry.ask) / 2;

    const bid = gbpUsdBid * usdTryBid;
    const ask = gbpUsdAsk * usdTryAsk;

    return { bid, ask };
}