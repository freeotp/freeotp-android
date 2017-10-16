package org.fedorahosted.freeotp;


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class TokenPersistenceTest {
    private final String issuer = "test@muster.com";
    private final String account = "18c5d0634ch53v";
    private final String secret = "base32secretString";
    private final String changed = "changed";
    private final String interval = "30";


    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private void addToken() {
        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.action_add), withContentDescription("Add Token"), isDisplayed()));
        actionMenuItemView.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.issuer), isDisplayed()));
        editText.perform(replaceText(issuer), closeSoftKeyboard());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.label), isDisplayed()));
        editText2.perform(replaceText(account), closeSoftKeyboard());

        ViewInteraction editText3 = onView(
                withId(R.id.secret));
        editText3.perform(scrollTo(), replaceText(secret), closeSoftKeyboard());

        ViewInteraction button = onView(
                allOf(withId(R.id.add), withText("Add"), isDisplayed()));
        button.perform(click());

    }

    private void openTokenMenu() {
        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu),
                        withParent(childAtPosition(
                                withId(R.id.grid),
                                0)),
                        isDisplayed()));
        imageView.perform(click());
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void tokenEditTest() {
        addToken();
        openTokenMenu();
        sleep();
        ViewInteraction textView = onView(
                allOf(withId(android.R.id.title), withText("Edit"), isDisplayed()));
        textView.perform(click());
        sleep();

        //Edit values
        ViewInteraction editText4 = onView(
                allOf(withId(R.id.issuer), isDisplayed()));
        editText4.perform(replaceText(issuer+changed), closeSoftKeyboard());
        ViewInteraction editText5 = onView(
                allOf(withId(R.id.label), isDisplayed()));
        editText5.perform(replaceText(secret+changed), closeSoftKeyboard());
        ViewInteraction button = onView(
                allOf(withId(R.id.save), withText("Save"), isDisplayed()));
        button.perform(click());

        //check values
        onView(withId(R.id.issuer)).check(matches(withText(issuer+changed)));
        onView(withId(R.id.label)).check(matches(withText(secret+changed)));
        //check also if click on token is still possible
        ViewInteraction tokenLayout = onView(
                allOf(childAtPosition(
                        withId(R.id.grid),
                        0),
                        isDisplayed()));
        tokenLayout.perform(click());
        onView(withId(R.id.code)).check((matches(not(withText("------")))));
        deleteToken();

    }

    private void deleteToken() {
        openTokenMenu();
        ViewInteraction textView2 = onView(
                allOf(withId(android.R.id.title), withText("Delete"), isDisplayed()));
        textView2.perform(click());

        ViewInteraction button3 = onView(
                allOf(withId(R.id.delete), withText("Delete"), isDisplayed()));
        button3.perform(click());
    }

    @Test
    public void tokenDeleteTest() {
        addToken();
        deleteToken();
        onView(withText(R.string.no_keys)).check(matches(isDisplayed()));
    }

    @Test
    public void tokenAddTest() {
        addToken();
        ViewInteraction tokenLayout = onView(
                allOf(childAtPosition(
                        withId(R.id.grid),
                        0),
                        isDisplayed()));
        tokenLayout.perform(click());
        onView(withId(R.id.code)).check((matches(not(withText("------")))));
        deleteToken();
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
