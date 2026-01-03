import nodemailer from 'nodemailer';

// Email configuration from environment variables
const EMAIL_HOST = process.env.EMAIL_HOST || 'smtp.gmail.com';
const EMAIL_PORT = parseInt(process.env.EMAIL_PORT || '587');
const EMAIL_SECURE = process.env.EMAIL_SECURE === 'true';
const EMAIL_USER = process.env.EMAIL_USER || '';
const EMAIL_PASS = process.env.EMAIL_PASS || '';
const EMAIL_FROM = process.env.EMAIL_FROM || 'Made in Braza <noreply@braza.com>';

// App URL for reset links
const APP_URL = process.env.APP_URL || 'https://braza.app.br';

// Create reusable transporter
const transporter = nodemailer.createTransport({
  host: EMAIL_HOST,
  port: EMAIL_PORT,
  secure: EMAIL_SECURE,
  auth: {
    user: EMAIL_USER,
    pass: EMAIL_PASS,
  },
});

// Check if email is configured
export function isEmailConfigured(): boolean {
  return !!(EMAIL_USER && EMAIL_PASS);
}

// Send password reset email
export async function sendPasswordResetEmail(
  to: string,
  nick: string,
  resetToken: string
): Promise<boolean> {
  if (!isEmailConfigured()) {
    console.warn('Email not configured. Cannot send password reset email.');
    return false;
  }

  const resetUrl = `${APP_URL}/web/reset-password?token=${resetToken}`;

  const mailOptions = {
    from: EMAIL_FROM,
    to,
    subject: 'Recuperar Senha - Made in Braza',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background: #1a1a2e; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
          .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
          .button { display: inline-block; background: #e94560; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
          .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
          .warning { background: #fff3cd; border: 1px solid #ffc107; padding: 10px; border-radius: 5px; margin-top: 20px; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>Made in Braza</h1>
          </div>
          <div class="content">
            <h2>Ola, ${nick}!</h2>
            <p>Voce solicitou a recuperacao de senha da sua conta.</p>
            <p>Clique no botao abaixo para criar uma nova senha:</p>
            <p style="text-align: center;">
              <a href="${resetUrl}" class="button">Redefinir Senha</a>
            </p>
            <p>Ou copie e cole este link no seu navegador:</p>
            <p style="word-break: break-all; background: #eee; padding: 10px; border-radius: 4px;">
              ${resetUrl}
            </p>
            <div class="warning">
              <strong>Atencao:</strong> Este link expira em 1 hora. Se voce nao solicitou esta recuperacao, ignore este email.
            </div>
          </div>
          <div class="footer">
            <p>Este email foi enviado automaticamente. Nao responda.</p>
            <p>&copy; ${new Date().getFullYear()} Made in Braza</p>
          </div>
        </div>
      </body>
      </html>
    `,
    text: `
Ola, ${nick}!

Voce solicitou a recuperacao de senha da sua conta no Made in Braza.

Clique no link abaixo para criar uma nova senha:
${resetUrl}

Este link expira em 1 hora.

Se voce nao solicitou esta recuperacao, ignore este email.

--
Made in Braza
    `.trim(),
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log(`Password reset email sent to ${to}`);
    return true;
  } catch (error) {
    console.error('Failed to send password reset email:', error);
    return false;
  }
}

// Verify email transporter connection
export async function verifyEmailConnection(): Promise<boolean> {
  if (!isEmailConfigured()) {
    return false;
  }

  try {
    await transporter.verify();
    console.log('Email transporter verified successfully');
    return true;
  } catch (error) {
    console.error('Email transporter verification failed:', error);
    return false;
  }
}
