package com.propslab.brand

/**
 * Every address the app can point at, in one place.
 *
 * Nothing here is a secret. The site, the update feed and the community destinations are
 * public URLs; the app sells from this site rather than a store, so the download link, the
 * checkout and the release feed all live on the same domain.
 *
 * A fork only has to change [SITE] — everything below is derived from it or overridable
 * from Settings at runtime.
 */
object Brand {

    const val APP = "Cheat Code"

    /** Said once, on the website and the manual: what the product is for. */
    const val TAGLINE = "Pro-level rigor. Plain words. No account needed."

    /** The deployment the app is sold from. Change this one line to ship a fork. */
    const val SITE = "https://propslab.vercel.app"

    const val WEBSITE = "$SITE/"
    const val MANUAL = "$SITE/manual.html"
    const val DOWNLOAD = "$SITE/download.html"
    const val REDDIT = "https://www.reddit.com/r/CheatCode/"
    const val DISCORD = "https://discord.gg/propslab"

    /**
     * A real inbox. Bug reports are prefilled and handed to the user's mail app, so this is
     * the address that actually receives them; it is not a placeholder.
     */
    const val EMAIL = "tap4500@gmail.com"

    /** Premium, and the one place it is sold. */
    const val PRICE_LABEL = "$5"
    const val PRICE_PERIOD = "month"
    const val FREE_NAME = "Free"
    const val PREMIUM_NAME = "Premium"

    /**
     * Creating a Checkout Session needs the Stripe secret key, so the app cannot build the
     * URL itself: this endpoint creates a fresh session and redirects to it.
     */
    const val CHECKOUT = "$SITE/api/go"

    /** Stripe's self-service portal, for cancelling or changing a card. */
    const val BILLING_PORTAL = "https://billing.stripe.com/p/login/propslab"

    /** The release feed this build reads for one-button updates. */
    const val UPDATE_FEED = "$SITE/latest.json"

    /** Optional sign-in, against our own Supabase project. The anon key is public by design. */
    const val AUTH_URL = "https://ktphbqvoycurlyxieurv.supabase.co"
    const val AUTH_ANON_KEY = "sb_publishable_M4-TLN2bUWdGlTmCE_E8aQ_8L8zqmKZ" // gitleaks:allow — publishable key
}
