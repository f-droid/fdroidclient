package org.fdroid.fdroid.panic;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.fdroid.fdroid.databinding.ActivityCalculatorBinding;

import java.util.regex.Pattern;

/**
 * A very hacky calculator which is barely functional.
 * It is just meant to pass a very casual inspection.
 */
public class CalculatorActivity extends AppCompatActivity {

    // binary operators
    private static final String TIMES = "×";
    private static final String DIVIDED = "÷";
    private static final String PLUS = "+";
    private static final String MINUS = "-";

    // unary operators
    private static final String PERCENT = "%";

    @Nullable
    private String lastOp;

    private ActivityCalculatorBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCalculatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
    }

    public void ce(View view) {
        // clear display
        binding.textView.setText(null);
    }

    public void c(View view) {
        // clear last character
        if (binding.textView.length() > 0) {
            String text = binding.textView.getText().toString();
            binding.textView.setText(text.substring(0, text.length() - 1));
        }
    }

    public void number(View view) {
        String number = ((Button) view).getText().toString();
        String newNumber = String.format("%s%s", binding.textView.getText(), number);
        // FIXME don't allow multiple commas
        String pin = String.valueOf(HidingManager.getUnhidePin(this));
        if (newNumber.equals(pin)) {
            // correct PIN was entered, show app launcher again
            HidingManager.show(this);
        }
        binding.textView.setText(newNumber);
    }

    public void op(View view) {
        String text = binding.textView.getText().toString();

        if (text.isEmpty()) {
            return;
        } else if (containsBinaryOperator(String.valueOf(text.charAt(text.length() - 1)))) {
            // last character was already binary operator, ignore
            return;
        }

        String op = ((Button) view).getText().toString();
        if (containsBinaryOperator(op)) {
            // remember binary operator
            lastOp = op;
            // add binary operator to display
            binding.textView.setText(String.format("%s%s", text, op));
        } else if (op.equals(PERCENT)) {
            double result;
            try {
                result = Double.parseDouble(eval(text));
            } catch (NumberFormatException e) {
                result = 0;
            }
            binding.textView.setText(toString(result / 100));
        } else if ("=".equals(op)) {
            binding.textView.setText(eval(text));
        } else {
            Toast.makeText(this, "Error: Unknown Operation", Toast.LENGTH_SHORT).show();
        }
    }

    private String eval(String s) {
        if (lastOp != null && s.contains(lastOp)) {
            // remember and reset binary operator
            String op = lastOp;
            lastOp = null;

            // extract binary operation
            String[] parts = s.split(Pattern.quote(op));
            double left;
            double right;
            try {
                left = Double.valueOf(parts[0]);
                right = Double.valueOf(parts[1]);
            } catch (NumberFormatException e) {
                return "";
            }

            // evaluate binary operation
            switch (op) {
                case PLUS:
                    return toString(left + right);
                case MINUS:
                    return toString(left - right);
                case TIMES:
                    return toString(left * right);
                case DIVIDED:
                    if (right == 0) return "";
                    return toString(left / right);
                default:
                    Toast.makeText(this, "Error: Unknown Operation", Toast.LENGTH_SHORT).show();
                    return s;
            }
        } else {
            return s;
        }
    }

    private boolean containsBinaryOperator(String s) {
        return s.contains(TIMES) || s.contains(DIVIDED) || s.contains(PLUS) || s.contains(MINUS);
    }

    private String toString(double d) {
        String s = String.valueOf(d);
        if (s.length() > 2 && s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

}
