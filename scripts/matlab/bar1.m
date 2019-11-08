measles = [55.64857748548341 53.82831116783483 51.71870241081558]';%???
mumps = [0.7157407167279669 0.7097418075501416 0.7114636811399635]';
chickenPox = [54.93283676875539 53.11856936028466 51.00723872967564]';
% Create a vertical bar chart using the bar function
figure
bar(1:12, [measles mumps chickenPox], 1);
% Set the axis limits
axis([0 13 0 40000])
set(gca, 'XTick', 1:12)
% Add title and axis labels
title('Childhood diseases by month')
xlabel('Month')
ylabel('Cases (in thousands)')
% Add a legend
legend('Measles', 'Mumps', 'Chicken pox')