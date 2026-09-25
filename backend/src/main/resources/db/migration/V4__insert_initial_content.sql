-- Flyway V4__insert_initial_content.sql
-- Insert initial content for static pages and FAQs
-- This script is safe to run multiple times by checking for existing records.

-- Insert home page if not exists
INSERT INTO page (title, slug, content, meta_title, meta_description, is_active)
SELECT 'Accueil', 'home', '<h1>Bienvenue sur Vollaile Link</h1><p>Plateforme d''approvisionnement en volailles au Sénégal.</p>', 'Accueil - Vollaile Link', 'Découvrez nos offres de volailles disponibles', true
WHERE NOT EXISTS (SELECT 1 FROM page WHERE slug = 'home');

-- Insert how-it-works page
INSERT INTO page (title, slug, content, meta_title, meta_description, is_active)
SELECT 'Comment ça marche', 'how-it-works', '<h1>Comment ça marche</h1><p>Explication du fonctionnement de la plateforme.</p>', 'Comment ça marche - Vollaile Link', 'Learn how Vollaile Link works', true
WHERE NOT EXISTS (SELECT 1 FROM page WHERE slug = 'how-it-works');

-- Insert about page
INSERT INTO page (title, slug, content, meta_title, meta_description, is_active)
SELECT 'À propos', 'about', '<h1>À propos de nous</h1><p>Informations sur Vollaile Link.</p>', 'À propos - Vollaile Link', 'About Vollaile Link', true
WHERE NOT EXISTS (SELECT 1 FROM page WHERE slug = 'about');

-- Insert service areas page
INSERT INTO page (title, slug, content, meta_title, meta_description, is_active)
SELECT 'Zones desservies', 'service-areas', '<h1>Zones desservies</h1><p>Nous desservons principalement Dakar et ses environs.</p>', 'Zones desservies - Vollaile Link', 'Service areas of Vollaile Link', true
WHERE NOT EXISTS (SELECT 1 FROM page WHERE slug = 'service-areas');

-- Insert contact page
INSERT INTO page (title, slug, content, meta_title, meta_description, is_active)
SELECT 'Contact', 'contact', '<h1>Contactez-nous</h1><p>Formulaire de contact ou informations de contact.</p>', 'Contact - Vollaile Link', 'Contact Vollaile Link', true
WHERE NOT EXISTS (SELECT 1 FROM page WHERE slug = 'contact');

-- Insert FAQs
INSERT INTO faq (question, answer, display_order, is_active)
SELECT 'Quels types de volailles sont disponibles ?', 'Nous présentons dans nos disponibilités les produits disponibles auprès de nos producteurs partenaires, selon les stocks et les dates de disponibilité.', 1, true
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = 'Quels types de volailles sont disponibles ?');

INSERT INTO faq (question, answer, display_order, is_active)
SELECT 'Comment passer une commande ?', 'Pour commander, envoyez votre demande à Vollaile Link via le formulaire de devis ou le WhatsApp professionnel. Notre équipe vérifie la disponibilité et vous recontacte pour confirmer les conditions.', 2, true
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = 'Comment passer une commande ?');

INSERT INTO faq (question, answer, display_order, is_active)
SELECT 'Les prix sont-ils négociables ?', 'Les prix affichés sont indicatifs et peuvent être soumis à confirmation. Vollaile Link vous communique les conditions définitives avant la validation de votre commande.', 3, true
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = 'Les prix sont-ils négociables ?');

INSERT INTO faq (question, answer, display_order, is_active)
SELECT 'Quelles sont les zones de livraison ?', 'Nous desservons principalement Dakar et sa région. Vollaile Link confirme la zone éligible, le délai et les frais éventuels avant la commande.', 4, true
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = 'Quelles sont les zones de livraison ?');

INSERT INTO faq (question, answer, display_order, is_active)
SELECT 'Comment puis-je proposer une disponibilité ?', 'Les producteurs ne disposent pas de compte public. Pour proposer une disponibilité, transmettez vos informations à l''équipe Vollaile Link via le canal convenu.', 5, true
WHERE NOT EXISTS (SELECT 1 FROM faq WHERE question = 'Comment puis-je proposer une disponibilité ?');