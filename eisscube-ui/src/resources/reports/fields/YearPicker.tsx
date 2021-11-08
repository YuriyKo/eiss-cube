import { useState, useEffect } from "react";
import { makeStyles, Theme } from '@material-ui/core/styles';
import { DatePicker } from "@material-ui/pickers";

const useStyles = makeStyles((theme: Theme) => ({ 
	date: {
		marginTop: theme.spacing(1),
		width: theme.spacing(20)
	}
}));

const YearPicker = (props: any) => {
	const { date, onChange } = props;
	const classes = useStyles();
	const [value, setValue] = useState(date);

	useEffect(() => {
		setValue(date);
	}, [date]);

	const changeDate = (new_date: any) => {
		onChange(new_date);
	};

	return (
		<DatePicker className={classes.date}
			autoOk
			variant='inline'
			views={['year']}
			label={false}
			disableFuture={true}
			value={value}
			onChange={changeDate}
		/>
	);
}

export default YearPicker;
